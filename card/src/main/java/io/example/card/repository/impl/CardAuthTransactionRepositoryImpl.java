package io.example.card.repository.impl;

import io.example.card.model.CardAuthTransaction;
import io.example.card.repository.CardAuthTransactionRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CardAuthTransactionRepositoryImpl implements CardAuthTransactionRepository {
  private final Pool pool;

  @Override
  public Future<CardAuthTransaction> insertPending(CardAuthTransaction txn) {
    return pool
        .preparedQuery(
            """
                INSERT INTO card_auth_transactions
                  (card_number, merchant_id, amount, currency, status, pos_entry_mode, mcc, idempotency_key)
                VALUES ($1, $2, $3, $4, 'PENDING', $5, $6, $7)
                RETURNING *
                """)
        .execute(Tuple.of(
            txn.getCardNumber(),
            txn.getMerchantId(),
            txn.getAmount(),
            txn.getCurrency() != null ? txn.getCurrency() : "IDR",
            txn.getPosEntryMode(),
            txn.getMcc(),
            txn.getIdempotencyKey()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardAuthTransaction> approve(String txnId, String authCode) {
    return pool
        .preparedQuery(
            """
                UPDATE card_auth_transactions
                SET status = 'APPROVED', auth_code = $1, settled_at = NOW()
                WHERE txn_id = $2::uuid AND status = 'PENDING'
                RETURNING *
                """)
        .execute(Tuple.of(authCode, txnId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardAuthTransaction> decline(String txnId, String declineCode) {
    return pool
        .preparedQuery(
            """
                UPDATE card_auth_transactions
                SET status = 'DECLINED', decline_code = $1
                WHERE txn_id = $2::uuid AND status = 'PENDING'
                RETURNING *
                """)
        .execute(Tuple.of(declineCode, txnId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardAuthTransaction> reverse(String txnId) {
    return pool
        .preparedQuery(
            """
                UPDATE card_auth_transactions
                SET status = 'REVERSED'
                WHERE txn_id = $1::uuid AND status = 'APPROVED'
                RETURNING *
                """)
        .execute(Tuple.of(txnId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardAuthTransaction> findByIdempotencyKey(String idempotencyKey) {
    return pool
        .preparedQuery(
            "SELECT * FROM card_auth_transactions WHERE idempotency_key = $1")
        .execute(Tuple.of(idempotencyKey))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardAuthTransaction> findById(String txnId) {
    return pool
        .preparedQuery(
            "SELECT * FROM card_auth_transactions WHERE txn_id = $1::uuid")
        .execute(Tuple.of(txnId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<List<CardAuthTransaction>> findByCardNumber(String cardNumber, int limit, int offset) {
    return pool
        .preparedQuery(
            "SELECT * FROM card_auth_transactions WHERE card_number = $1 ORDER BY txn_time DESC LIMIT $2 OFFSET $3")
        .execute(Tuple.of(cardNumber, limit, offset))
        .map(this::mapToList);
  }

  @Override
  public Future<Long> countRecentByCardNumber(String cardNumber, int windowSeconds) {
    return pool
        .preparedQuery(
            """
                SELECT COUNT(*) AS cnt FROM card_auth_transactions
                WHERE card_number = $1
                  AND txn_time > NOW() - ($2 || ' seconds')::interval
                """)
        .execute(Tuple.of(cardNumber, windowSeconds))
        .map(rows -> rows.iterator().next().getLong("cnt"));
  }

  @Override
  public Future<Integer> updateRiskScore(String txnId, Integer riskScore) {
    return pool
        .preparedQuery(
            "UPDATE card_auth_transactions SET risk_score = $1 WHERE txn_id = $2::uuid")
        .execute(Tuple.of(riskScore, txnId))
        .map(RowSet::rowCount);
  }

  private CardAuthTransaction mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? CardAuthTransaction.fromRow(rows.iterator().next()) : null;
  }

  private List<CardAuthTransaction> mapToList(RowSet<Row> rows) {
    List<CardAuthTransaction> list = new ArrayList<>();
    rows.forEach(row -> list.add(CardAuthTransaction.fromRow(row)));
    return list;
  }
}
