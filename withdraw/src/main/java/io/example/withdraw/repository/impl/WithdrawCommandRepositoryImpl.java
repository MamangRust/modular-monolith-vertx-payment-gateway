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
    String sql = """
        INSERT INTO withdraws (card_number, withdraw_amount, withdraw_time, status, created_at, updated_at)
        VALUES ($1, $2, CURRENT_TIMESTAMP, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING *
        """;
    return pool.preparedQuery(sql)
        .execute(Tuple.of(req.getCardNumber(), req.getWithdrawAmount()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> updateWithdraw(UpdateWithdrawRequest req) {
    String sql = """
        UPDATE withdraws SET card_number = $2, withdraw_amount = $3, updated_at = CURRENT_TIMESTAMP
        WHERE withdraw_id = $1 AND deleted_at IS NULL RETURNING *
        """;
    return pool.preparedQuery(sql)
        .execute(Tuple.of(req.getWithdrawId(), req.getCardNumber(), req.getWithdrawAmount()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> updateWithdrawStatus(UpdateWithdrawStatus req) {
    String sql = "UPDATE withdraws SET status = $2, updated_at = CURRENT_TIMESTAMP WHERE withdraw_id = $1 AND deleted_at IS NULL RETURNING *";
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
