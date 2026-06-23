package io.example.card.repository.impl;

import io.example.card.repository.CardDashboardBalanceRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CardDashboardBalanceRepositoryImpl implements CardDashboardBalanceRepository {
  private final Pool pool;

  @Override
  public Future<Long> getTotalBalances() {
    return pool.query("""
        SELECT COALESCE(SUM(s.total_balance), 0)::bigint
        FROM saldos s JOIN cards c ON s.card_number = c.card_number
        WHERE s.deleted_at IS NULL AND c.deleted_at IS NULL
        """)
        .execute()
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong(0) : 0L);
  }

  @Override
  public Future<Long> getTotalBalanceByCardNumber(String cardNumber) {
    return pool.preparedQuery("""
        SELECT COALESCE(SUM(s.total_balance), 0)::bigint
        FROM saldos s JOIN cards c ON s.card_number = c.card_number
        WHERE s.deleted_at IS NULL AND c.deleted_at IS NULL AND c.card_number = $1
        """)
        .execute(Tuple.of(cardNumber))
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong(0) : 0L);
  }
}
