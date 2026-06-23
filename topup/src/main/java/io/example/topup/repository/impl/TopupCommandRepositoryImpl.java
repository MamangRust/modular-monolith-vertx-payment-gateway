package io.example.topup.repository.impl;

import io.example.topup.model.Topup;
import io.example.topup.repository.TopupCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import io.example.topup.domain.requests.topup.CreateTopupRequest;
import io.example.topup.domain.requests.topup.UpdateTopupAmount;
import io.example.topup.domain.requests.topup.UpdateTopupRequest;
import io.example.topup.domain.requests.topup.UpdateTopupStatus;

@RequiredArgsConstructor
public class TopupCommandRepositoryImpl implements TopupCommandRepository {
  private final Pool pool;

  @Override
  public Future<Topup> createTopup(CreateTopupRequest req) {
    String sql = """
        INSERT INTO topups (card_number, topup_no, topup_amount, topup_method, topup_time, status, created_at, updated_at)
        VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING *
        """;
    return pool.preparedQuery(sql)
        .execute(Tuple.of(req.getCardNumber(), req.getTopupNo(), req.getTopupAmount(), req.getTopupMethod()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Topup> updateTopup(UpdateTopupRequest req) {
    String sql = """
        UPDATE topups SET card_number = $2, topup_amount = $3, topup_method = $4, updated_at = CURRENT_TIMESTAMP
        WHERE topup_id = $1 AND deleted_at IS NULL RETURNING *
        """;
    return pool.preparedQuery(sql)
        .execute(Tuple.of(req.getTopupId(), req.getCardNumber(), req.getTopupAmount(), req.getTopupMethod()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Topup> updateTopupAmount(UpdateTopupAmount req) {
    String sql = "UPDATE topups SET topup_amount = $2, updated_at = CURRENT_TIMESTAMP WHERE topup_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(req.getTopupId(), req.getTopupAmount())).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Topup> updateTopupStatus(UpdateTopupStatus req) {
    String sql = "UPDATE topups SET status = $2, updated_at = CURRENT_TIMESTAMP WHERE topup_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(req.getTopupId(), req.getStatus())).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Topup> trashTopup(Integer id) {
    String sql = "UPDATE topups SET deleted_at = CURRENT_TIMESTAMP WHERE topup_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Topup> restoreTopup(Integer id) {
    String sql = "UPDATE topups SET deleted_at = NULL WHERE topup_id = $1 AND deleted_at IS NOT NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Boolean> deleteTopupPermanently(Integer id) {
    return pool.preparedQuery("DELETE FROM topups WHERE topup_id = $1").execute(Tuple.of(id)).map(rows -> true);
  }

  @Override
  public Future<Integer> restoreAllTopups() {
    return pool.query("UPDATE topups SET deleted_at = NULL WHERE deleted_at IS NOT NULL").execute()
        .map(rows -> rows.rowCount());
  }

  @Override
  public Future<Integer> deleteAllPermanentTopups() {
    return pool.query("DELETE FROM topups WHERE deleted_at IS NOT NULL").execute().map(rows -> rows.rowCount());
  }

  private Topup mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Topup.fromRow(rows.iterator().next()) : null;
  }
}
