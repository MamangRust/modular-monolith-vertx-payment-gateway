package io.example.card.repository.impl;

import io.example.card.model.CardPayment;
import io.example.card.repository.CardPaymentRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CardPaymentRepositoryImpl implements CardPaymentRepository {
  private final Pool pool;

  @Override
  public Future<CardPayment> findByReferenceId(String referenceId) {
    return pool
        .preparedQuery(
            "SELECT * FROM card_payments WHERE reference_id = $1")
        .execute(Tuple.of(referenceId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardPayment> insertPayment(CardPayment payment) {
    return pool
        .preparedQuery(
            """
                INSERT INTO card_payments
                  (reference_id, card_number, amount, payment_channel, statement_id)
                VALUES ($1, $2, $3, $4, $5)
                RETURNING *
                """)
        .execute(Tuple.of(
            payment.getReferenceId(),
            payment.getCardNumber(),
            payment.getAmount(),
            payment.getPaymentChannel(),
            payment.getStatementId()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<List<CardPayment>> findByCardNumber(String cardNumber, int limit, int offset) {
    return pool
        .preparedQuery(
            "SELECT * FROM card_payments WHERE card_number = $1 ORDER BY payment_time DESC LIMIT $2 OFFSET $3")
        .execute(Tuple.of(cardNumber, limit, offset))
        .map(this::mapToList);
  }

  @Override
  public Future<Integer> countByCardNumber(String cardNumber) {
    return pool
        .preparedQuery("SELECT COUNT(*) AS cnt FROM card_payments WHERE card_number = $1")
        .execute(Tuple.of(cardNumber))
        .map(rows -> rows.iterator().next().getInteger("cnt"));
  }

  @Override
  public Future<Long> totalPaymentsByCardNumber(String cardNumber) {
    return pool
        .preparedQuery("SELECT COALESCE(SUM(amount), 0) AS total FROM card_payments WHERE card_number = $1 AND status = 'POSTED'")
        .execute(Tuple.of(cardNumber))
        .map(rows -> rows.iterator().next().getLong("total"));
  }

  @Override
  public Future<CardPayment> findById(String paymentId) {
    return pool
        .preparedQuery("SELECT * FROM card_payments WHERE payment_id = $1::uuid")
        .execute(Tuple.of(paymentId))
        .map(this::mapSingleOrNull);
  }

  private CardPayment mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? CardPayment.fromRow(rows.iterator().next()) : null;
  }

  private List<CardPayment> mapToList(RowSet<Row> rows) {
    List<CardPayment> list = new ArrayList<>();
    rows.forEach(row -> list.add(CardPayment.fromRow(row)));
    return list;
  }
}
