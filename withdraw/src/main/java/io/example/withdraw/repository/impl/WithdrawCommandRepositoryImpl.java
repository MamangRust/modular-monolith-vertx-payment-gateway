package io.example.withdraw.repository.impl;

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
  public Future<Withdraw> createWithdraw(String card, long amount) {
    String sql = """
        INSERT INTO withdraws (card_number, withdraw_amount, withdraw_time, status, created_at, updated_at)
        VALUES ($1, $2, CURRENT_TIMESTAMP, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING *
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(card, amount)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> updateWithdraw(int id, String card, long amount) {
    String sql = """
        UPDATE withdraws SET card_number = $2, withdraw_amount = $3, updated_at = CURRENT_TIMESTAMP
        WHERE withdraw_id = $1 AND deleted_at IS NULL RETURNING *
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(id, card, amount)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> updateWithdrawStatus(int id, String status) {
    String sql = "UPDATE withdraws SET status = $2, updated_at = CURRENT_TIMESTAMP WHERE withdraw_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id, status)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> trashWithdraw(int id) {
    String sql = "UPDATE withdraws SET deleted_at = CURRENT_TIMESTAMP WHERE withdraw_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> restoreWithdraw(int id) {
    String sql = "UPDATE withdraws SET deleted_at = NULL WHERE withdraw_id = $1 AND deleted_at IS NOT NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Void> deleteWithdrawPermanently(int id) {
    return pool.preparedQuery("DELETE FROM withdraws WHERE withdraw_id = $1").execute(Tuple.of(id)).mapEmpty();
  }

  @Override
  public Future<Void> restoreAllWithdraws() {
    return pool.query("UPDATE withdraws SET deleted_at = NULL WHERE deleted_at IS NOT NULL").execute().mapEmpty();
  }

  @Override
  public Future<Void> deleteAllPermanentWithdraws() {
    return pool.query("DELETE FROM withdraws WHERE deleted_at IS NOT NULL").execute().mapEmpty();
  }

  private Withdraw mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Withdraw.fromRow(rows.iterator().next()) : null;
  }
}
