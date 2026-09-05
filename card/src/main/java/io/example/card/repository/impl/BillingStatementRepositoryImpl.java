package io.example.card.repository.impl;

import io.example.card.model.BillingStatement;
import io.example.card.repository.BillingStatementRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class BillingStatementRepositoryImpl implements BillingStatementRepository {
  private final Pool pool;

  @Override
  public Future<BillingStatement> insertStatement(BillingStatement stmt) {
    return pool
        .preparedQuery(
            """
                INSERT INTO card_billing_statements
                  (card_number, statement_date, due_date, opening_balance, purchases, cash_advances,
                   payments, fees, interest_charged, closing_balance, minimum_payment, payment_status)
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, 'UNPAID')
                ON CONFLICT (card_number, statement_date) DO UPDATE SET
                  opening_balance = EXCLUDED.opening_balance,
                  purchases = EXCLUDED.purchases,
                  cash_advances = EXCLUDED.cash_advances,
                  payments = EXCLUDED.payments,
                  fees = EXCLUDED.fees,
                  interest_charged = EXCLUDED.interest_charged,
                  closing_balance = EXCLUDED.closing_balance,
                  minimum_payment = EXCLUDED.minimum_payment
                RETURNING *
                """)
        .execute(Tuple.of(
            stmt.getCardNumber(),
            stmt.getStatementDate(),
            stmt.getDueDate(),
            stmt.getOpeningBalance(),
            stmt.getPurchases(),
            stmt.getCashAdvances(),
            stmt.getPayments(),
            stmt.getFees(),
            stmt.getInterestCharged(),
            stmt.getClosingBalance(),
            stmt.getMinimumPayment()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<BillingStatement> findByCardAndCycle(String cardNumber, LocalDate statementDate) {
    return pool
        .preparedQuery(
            "SELECT * FROM card_billing_statements WHERE card_number = $1 AND statement_date = $2")
        .execute(Tuple.of(cardNumber, statementDate))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<List<BillingStatement>> findByCardNumber(String cardNumber, int limit, int offset) {
    return pool
        .preparedQuery(
            "SELECT * FROM card_billing_statements WHERE card_number = $1 ORDER BY statement_date DESC LIMIT $2 OFFSET $3")
        .execute(Tuple.of(cardNumber, limit, offset))
        .map(this::mapToList);
  }

  @Override
  public Future<BillingStatement> updatePaymentStatus(Integer statementId, String paymentStatus) {
    return pool
        .preparedQuery(
            "UPDATE card_billing_statements SET payment_status = $1 WHERE statement_id = $2 RETURNING *")
        .execute(Tuple.of(paymentStatus, statementId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<BillingStatement> findLatestByCardNumber(String cardNumber) {
    return pool
        .preparedQuery(
            "SELECT * FROM card_billing_statements WHERE card_number = $1 ORDER BY statement_date DESC LIMIT 1")
        .execute(Tuple.of(cardNumber))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Integer> countByCardNumber(String cardNumber) {
    return pool
        .preparedQuery("SELECT COUNT(*) AS cnt FROM card_billing_statements WHERE card_number = $1")
        .execute(Tuple.of(cardNumber))
        .map(rows -> rows.iterator().next().getInteger("cnt"));
  }

  private BillingStatement mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? BillingStatement.fromRow(rows.iterator().next()) : null;
  }

  private List<BillingStatement> mapToList(RowSet<Row> rows) {
    List<BillingStatement> list = new ArrayList<>();
    rows.forEach(row -> list.add(BillingStatement.fromRow(row)));
    return list;
  }
}
