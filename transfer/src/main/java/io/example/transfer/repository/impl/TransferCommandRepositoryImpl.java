package io.example.transfer.repository.impl;

import io.example.transfer.model.Transfer;
import io.example.transfer.repository.TransferCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class TransferCommandRepositoryImpl implements TransferCommandRepository {
  private final Pool pool;

  public TransferCommandRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  @Override
  public Future<Transfer> createTransfer(String from, String to, long amount) {
    String sql = """
        INSERT INTO transfers (transfer_from, transfer_to, transfer_amount, transfer_time, status, created_at, updated_at)
        VALUES ($1, $2, $3, CURRENT_TIMESTAMP, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING *
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(from, to, amount)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Transfer> updateTransfer(int id, String from, String to, long amount) {
    String sql = """
        UPDATE transfers SET transfer_from = $2, transfer_to = $3, transfer_amount = $4, updated_at = CURRENT_TIMESTAMP
        WHERE transfer_id = $1 AND deleted_at IS NULL RETURNING *
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(id, from, to, amount)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Transfer> updateTransferAmount(int id, long amount) {
    String sql = "UPDATE transfers SET transfer_amount = $2, updated_at = CURRENT_TIMESTAMP WHERE transfer_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id, amount)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Transfer> updateTransferStatus(int id, String status) {
    String sql = "UPDATE transfers SET status = $2, updated_at = CURRENT_TIMESTAMP WHERE transfer_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id, status)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Transfer> trashTransfer(int id) {
    String sql = "UPDATE transfers SET deleted_at = CURRENT_TIMESTAMP WHERE transfer_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Transfer> restoreTransfer(int id) {
    String sql = "UPDATE transfers SET deleted_at = NULL WHERE transfer_id = $1 AND deleted_at IS NOT NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Void> deleteTransferPermanently(int id) {
    return pool.preparedQuery("DELETE FROM transfers WHERE transfer_id = $1").execute(Tuple.of(id)).mapEmpty();
  }

  @Override
  public Future<Void> restoreAllTransfers() {
    return pool.query("UPDATE transfers SET deleted_at = NULL WHERE deleted_at IS NOT NULL").execute().mapEmpty();
  }

  @Override
  public Future<Void> deleteAllPermanentTransfers() {
    return pool.query("DELETE FROM transfers WHERE deleted_at IS NOT NULL").execute().mapEmpty();
  }

  private Transfer mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Transfer.fromRow(rows.iterator().next()) : null;
  }
}
