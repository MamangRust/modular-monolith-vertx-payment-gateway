package io.example.card.repository.impl;

import io.example.card.repository.CardDashboardTransactionRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

public class CardDashboardTransactionRepositoryImpl implements CardDashboardTransactionRepository {
  private final Pool pool;

  public CardDashboardTransactionRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  @Override
  public Future<Long> getTotalTransactionAmount() {
    return pool.query("""
        SELECT COALESCE(SUM(amount), 0)::bigint
        FROM transactions WHERE deleted_at IS NULL
        """)
        .execute()
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong(0) : 0L);
  }

  @Override
  public Future<Long> getTotalTransactionAmountByCardNumber(String cardNumber) {
    return pool.preparedQuery("""
        SELECT COALESCE(SUM(amount), 0)::bigint
        FROM transactions WHERE card_number = $1 AND deleted_at IS NULL
        """)
        .execute(Tuple.of(cardNumber))
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong(0) : 0L);
  }
}
