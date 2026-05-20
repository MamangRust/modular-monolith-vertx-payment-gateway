package io.example.card.repository.impl;

import io.example.card.repository.CardDashboardWithdrawRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

public class CardDashboardWithdrawRepositoryImpl implements CardDashboardWithdrawRepository {
  private final Pool pool;

  public CardDashboardWithdrawRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  @Override
  public Future<Long> getTotalWithdrawAmount() {
    return pool.query("""
        SELECT COALESCE(SUM(withdraw_amount), 0)::bigint
        FROM withdraws WHERE deleted_at IS NULL
        """)
        .execute()
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong(0) : 0L);
  }

  @Override
  public Future<Long> getTotalWithdrawAmountByCardNumber(String cardNumber) {
    return pool.preparedQuery("""
        SELECT COALESCE(SUM(withdraw_amount), 0)::bigint
        FROM withdraws WHERE card_number = $1 AND deleted_at IS NULL
        """)
        .execute(Tuple.of(cardNumber))
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong(0) : 0L);
  }
}
