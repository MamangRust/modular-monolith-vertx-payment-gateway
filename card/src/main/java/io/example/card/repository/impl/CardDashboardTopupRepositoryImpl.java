package io.example.card.repository.impl;

import io.example.card.repository.CardDashboardTopupRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

public class CardDashboardTopupRepositoryImpl implements CardDashboardTopupRepository {
  private final Pool pool;

  public CardDashboardTopupRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  @Override
  public Future<Long> getTotalTopAmount() {
    return pool.query("""
        SELECT COALESCE(SUM(topup_amount), 0)::bigint
        FROM topups WHERE deleted_at IS NULL
        """)
        .execute()
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong(0) : 0L);
  }

  @Override
  public Future<Long> getTotalTopupAmountByCardNumber(String cardNumber) {
    return pool.preparedQuery("""
        SELECT COALESCE(SUM(topup_amount), 0)::bigint
        FROM topups WHERE card_number = $1 AND deleted_at IS NULL
        """)
        .execute(Tuple.of(cardNumber))
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong(0) : 0L);
  }
}
