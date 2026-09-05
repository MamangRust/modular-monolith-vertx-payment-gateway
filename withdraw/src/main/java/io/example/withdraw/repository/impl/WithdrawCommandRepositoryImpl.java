package io.example.withdraw.repository.impl;

import io.example.withdraw.domain.requests.CreateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateWithdrawStatus;
import io.example.withdraw.model.Withdraw;
import io.example.withdraw.repository.WithdrawCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WithdrawCommandRepositoryImpl implements WithdrawCommandRepository {
  private final Pool pool;

  @Override
  public Future<Withdraw> createWithdraw(CreateWithdrawRequest req) {
    return createWithdraw(req, 10_000_000L);
  }

  @Override
  public Future<Withdraw> createWithdraw(CreateWithdrawRequest req, long dailyLimit) {
    String sql = """
        WITH card_lock AS (
          SELECT pg_advisory_xact_lock(hashtextextended($1, 0))
        ), daily_total AS (
          SELECT COALESCE(SUM(w.withdraw_amount), 0) AS total_amount
          FROM withdraws w, card_lock
          WHERE w.card_number = $1 AND w.status IN ('pending', 'success') AND w.deleted_at IS NULL
            AND w.withdraw_time >= CURRENT_DATE
            AND w.withdraw_time < CURRENT_DATE + INTERVAL '1 day'
        )
        INSERT INTO withdraws (card_number, withdraw_amount, idempotency_key, withdraw_time, status, created_at, updated_at)
        SELECT $1, $2, $3, CURRENT_TIMESTAMP, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        FROM daily_total
        WHERE daily_total.total_amount + $2 <= $4
        ON CONFLICT (idempotency_key)
        WHERE idempotency_key IS NOT NULL AND deleted_at IS NULL DO NOTHING
        RETURNING *
        """;
    String idempotencyKey = req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()
        ? req.getIdempotencyKey() : null;
    return pool.preparedQuery(sql)
        .execute(Tuple.of(req.getCardNumber(), req.getWithdrawAmount(), idempotencyKey, dailyLimit))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> findByIdempotencyKey(String idempotencyKey) {
    String sql = "SELECT * FROM withdraws WHERE idempotency_key = $1 AND deleted_at IS NULL LIMIT 1";
    return pool.preparedQuery(sql)
        .execute(Tuple.of(idempotencyKey))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> updateWithdraw(UpdateWithdrawRequest req) {
    return updateWithdraw(req, 10_000_000L);
  }

  @Override
  public Future<Withdraw> updateWithdraw(UpdateWithdrawRequest req, long dailyLimit) {
    String sql = """
        WITH card_lock AS (
          SELECT pg_advisory_xact_lock(hashtextextended($2, 0))
        ), daily_total AS (
          SELECT COALESCE(SUM(w.withdraw_amount), 0) AS total_amount
          FROM withdraws w, card_lock
          WHERE w.card_number = $2 AND w.withdraw_id <> $1
            AND w.status IN ('pending', 'success') AND w.deleted_at IS NULL
            AND w.withdraw_time >= CURRENT_DATE
            AND w.withdraw_time < CURRENT_DATE + INTERVAL '1 day'
        )
        UPDATE withdraws
        SET card_number = COALESCE(NULLIF($2, ''), card_number),
            withdraw_amount = COALESCE(NULLIF($3, 0), withdraw_amount),
            updated_at = CURRENT_TIMESTAMP
        FROM daily_total
        WHERE withdraw_id = $1 AND deleted_at IS NULL AND daily_total.total_amount + $3 <= $4

        RETURNING withdraws.*
        """;
    return pool.preparedQuery(sql)
        .execute(Tuple.of(req.getWithdrawId(), req.getCardNumber(), req.getWithdrawAmount(), dailyLimit))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> updateWithdrawStatus(UpdateWithdrawStatus req) {
    String sql = "UPDATE withdraws SET status = COALESCE(NULLIF($2, ''), status), updated_at = CURRENT_TIMESTAMP WHERE withdraw_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(req.getWithdrawId(), req.getStatus())).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> trashWithdraw(Integer id) {
    String sql = "UPDATE withdraws SET deleted_at = CURRENT_TIMESTAMP WHERE withdraw_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> restoreWithdraw(Integer id) {
    String sql = "UPDATE withdraws SET deleted_at = NULL WHERE withdraw_id = $1 AND deleted_at IS NOT NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id)).map(this::mapSingleOrNull);
  }

  public Future<Boolean> deleteWithdrawPermanently(Integer withdrawId) {
    return pool
        .preparedQuery("DELETE FROM withdraws WHERE withdraw_id = $1 AND deleted_at IS NOT NULL")
        .execute(Tuple.of(withdrawId))
        .map(rowSet -> rowSet.rowCount() > 0);
  }

  public Future<Integer> restoreAllWithdraws() {
    return pool
        .query("UPDATE withdraws SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
        .execute()
        .map(RowSet::rowCount);
  }

  public Future<Integer> deleteAllPermanentWithdraws() {
    return pool
        .query("DELETE FROM withdraws WHERE deleted_at IS NOT NULL")
        .execute()
        .map(RowSet::rowCount);
  }

  private Withdraw mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Withdraw.fromRow(rows.iterator().next()) : null;
  }
}
