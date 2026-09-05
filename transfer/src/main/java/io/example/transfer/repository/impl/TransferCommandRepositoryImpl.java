package io.example.transfer.repository.impl;

import io.example.transfer.model.Transfer;
import io.example.transfer.repository.TransferCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransferCommandRepositoryImpl implements TransferCommandRepository {
  private final Pool pool;

  @Override
  public Future<Transfer> createTransfer(String from, String to, long amount, String idempotencyKey) {
    String sql = """
        INSERT INTO transfers (transfer_from, transfer_to, transfer_amount, idempotency_key, transfer_time, status, created_at, updated_at)
        VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (idempotency_key) WHERE idempotency_key IS NOT NULL AND deleted_at IS NULL DO NOTHING
        RETURNING *
        """;
    String key = idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : null;
    return pool.preparedQuery(sql).execute(Tuple.of(from, to, amount, key)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Transfer> findByIdempotencyKey(String idempotencyKey) {
    String sql = "SELECT * FROM transfers WHERE idempotency_key = $1 AND deleted_at IS NULL LIMIT 1";
    return pool.preparedQuery(sql).execute(Tuple.of(idempotencyKey)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Transfer> updateTransfer(int id, String from, String to, long amount) {
    String sql = """
        UPDATE transfers
        SET transfer_from = COALESCE(NULLIF($2, ''), transfer_from),
            transfer_to = COALESCE(NULLIF($3, ''), transfer_to),
            transfer_amount = COALESCE(NULLIF($4, 0), transfer_amount),
            updated_at = CURRENT_TIMESTAMP
        WHERE transfer_id = $1 AND deleted_at IS NULL RETURNING *
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(id,
        from != null ? from : "",
        to != null ? to : "",
        amount)).map(this::mapSingleOrNull);
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

  public Future<Boolean> deleteTransferPermanently(int transferId) {
    return pool
        .preparedQuery("DELETE FROM transfers WHERE transfer_id = $1 AND deleted_at IS NOT NULL")
        .execute(Tuple.of(transferId))
        .map(rowSet -> rowSet.rowCount() > 0);
  }

  public Future<Integer> restoreAllTransfers() {
    return pool
        .query("UPDATE transfers SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
        .execute()
        .map(RowSet::rowCount);
  }

  public Future<Integer> deleteAllPermanentTransfers() {
    return pool
        .query("DELETE FROM transfers WHERE deleted_at IS NOT NULL")
        .execute()
        .map(RowSet::rowCount);
  }

  private Transfer mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Transfer.fromRow(rows.iterator().next()) : null;
  }
}
