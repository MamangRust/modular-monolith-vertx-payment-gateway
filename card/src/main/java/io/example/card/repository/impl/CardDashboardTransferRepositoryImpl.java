package io.example.card.repository.impl;

import io.example.card.repository.CardDashboardTransferRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CardDashboardTransferRepositoryImpl implements CardDashboardTransferRepository {
  private final Pool pool;

  @Override
  public Future<Long> getTotalTransferAmount() {
    return pool.query("""
        SELECT COALESCE(SUM(transfer_amount), 0)::bigint
        FROM transfers WHERE deleted_at IS NULL
        """)
        .execute()
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong(0) : 0L);
  }

  @Override
  public Future<Long> getTotalTransferAmountBySender(String senderCardNumber) {
    return pool.preparedQuery("""
        SELECT COALESCE(SUM(transfer_amount), 0)::bigint
        FROM transfers WHERE transfer_from = $1 AND deleted_at IS NULL
        """)
        .execute(Tuple.of(senderCardNumber))
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong(0) : 0L);
  }

  @Override
  public Future<Long> getTotalTransferAmountByReceiver(String receiverCardNumber) {
    return pool.preparedQuery("""
        SELECT COALESCE(SUM(transfer_amount), 0)::bigint
        FROM transfers WHERE transfer_to = $1 AND deleted_at IS NULL
        """)
        .execute(Tuple.of(receiverCardNumber))
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong(0) : 0L);
  }
}
