package io.example.card.repository.impl;

import io.example.card.model.CardCreditAccount;
import io.example.card.repository.CardCreditAccountRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CardCreditAccountRepositoryImpl implements CardCreditAccountRepository {
  private final Pool pool;

  @Override
  public Future<CardCreditAccount> findByCardNumber(String cardNumber) {
    return pool
        .preparedQuery(
            "SELECT * FROM card_credit_accounts WHERE card_number = $1")
        .execute(Tuple.of(cardNumber))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardCreditAccount> createAccount(String cardNumber, Long creditLimit,
                                                  Integer billingCycleDay, Integer annualRateBps) {
    return pool
        .preparedQuery(
            """
                INSERT INTO card_credit_accounts (card_number, credit_limit, used_credit, billing_cycle_day, annual_rate_bps)
                VALUES ($1, $2, 0, $3, $4)
                RETURNING *
                """)
        .execute(Tuple.of(cardNumber, creditLimit, billingCycleDay, annualRateBps))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardCreditAccount> decrementAvailableCredit(String cardNumber, Long amount) {
    return pool
        .preparedQuery(
            """
                UPDATE card_credit_accounts
                SET used_credit = used_credit + $1, updated_at = NOW()
                WHERE card_number = $2
                  AND (credit_limit - used_credit) >= $1
                  AND status = 'ACTIVE'
                RETURNING *
                """)
        .execute(Tuple.of(amount, cardNumber))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardCreditAccount> releaseCredit(String cardNumber, Long amount) {
    return pool
        .preparedQuery(
            """
                UPDATE card_credit_accounts
                SET used_credit = GREATEST(used_credit - $1, 0), updated_at = NOW()
                WHERE card_number = $2
                RETURNING *
                """)
        .execute(Tuple.of(amount, cardNumber))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardCreditAccount> updateStatus(String cardNumber, String status) {
    return pool
        .preparedQuery(
            """
                UPDATE card_credit_accounts
                SET status = $1, updated_at = NOW()
                WHERE card_number = $2
                RETURNING *
                """)
        .execute(Tuple.of(status, cardNumber))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<List<CardCreditAccount>> findAccountsDueForBilling(Integer cycleDay) {
    return pool
        .preparedQuery(
            "SELECT * FROM card_credit_accounts WHERE billing_cycle_day = $1 AND status = 'ACTIVE'")
        .execute(Tuple.of(cycleDay))
        .map(this::mapToList);
  }

  @Override
  public Future<CardCreditAccount> setCreditLimit(String cardNumber, Long creditLimit) {
    return pool
        .preparedQuery(
            """
                UPDATE card_credit_accounts
                SET credit_limit = $1, updated_at = NOW()
                WHERE card_number = $2
                RETURNING *
                """)
        .execute(Tuple.of(creditLimit, cardNumber))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardCreditAccount> adjustCreditLimit(String cardNumber, Long delta) {
    return pool
        .preparedQuery(
            """
                UPDATE card_credit_accounts
                SET credit_limit = GREATEST(credit_limit + $1, 0), updated_at = NOW()
                WHERE card_number = $2
                RETURNING *
                """)
        .execute(Tuple.of(delta, cardNumber))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Boolean> deleteByCardNumber(String cardNumber) {
    return pool
        .preparedQuery("DELETE FROM card_credit_accounts WHERE card_number = $1")
        .execute(Tuple.of(cardNumber))
        .map(rows -> rows.rowCount() > 0);
  }

  private CardCreditAccount mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? CardCreditAccount.fromRow(rows.iterator().next()) : null;
  }

  private List<CardCreditAccount> mapToList(RowSet<Row> rows) {
    List<CardCreditAccount> list = new ArrayList<>();
    rows.forEach(row -> list.add(CardCreditAccount.fromRow(row)));
    return list;
  }
}
