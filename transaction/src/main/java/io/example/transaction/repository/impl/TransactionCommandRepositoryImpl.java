package io.example.transaction.repository.impl;

import io.example.transaction.model.Transaction;
import io.example.transaction.repository.TransactionCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;

public class TransactionCommandRepositoryImpl implements TransactionCommandRepository {
  private final Pool pool;

  public TransactionCommandRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  @Override
  public Future<Transaction> createTransaction(CreateTransactionRequest req) {
    String sql = """
        INSERT INTO transactions (card_number, amount, payment_method, merchant_id, transaction_time, status, created_at, updated_at)
        VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING *
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(
        req.getCardNumber(),
        req.getAmount(),
        req.getPaymentMethod(),
        (int) req.getMerchantId()))
        .map(this::mapSingle);
  }

  @Override
  public Future<Transaction> updateTransaction(UpdateTransactionRequest req) {
    String sql = """
        UPDATE transactions SET card_number = $2, amount = $3, payment_method = $4, merchant_id = $5, updated_at = CURRENT_TIMESTAMP
        WHERE transaction_id = $1 AND deleted_at IS NULL RETURNING *
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(
        req.getTransactionId(),
        req.getCardNumber(),
        req.getAmount(),
        req.getPaymentMethod(),
        (int) req.getMerchantId()))
        .map(this::mapSingle);
  }

  @Override
  public Future<Transaction> updateTransactionStatus(Integer id, String status) {
    String sql = "UPDATE transactions SET status = $2, updated_at = CURRENT_TIMESTAMP WHERE transaction_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id, status)).map(this::mapSingle);
  }

  @Override
  public Future<Transaction> getTransactionById(Integer req) {
    String sql = "SELECT * FROM transactions WHERE transaction_id = $1 AND deleted_at IS NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(req))
        .map(this::mapSingle);
  }

  @Override
  public Future<Transaction> getTrashedTransactionById(Integer req) {
    String sql = "SELECT * FROM transactions WHERE transaction_id = $1 AND deleted_at IS NOT NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(req))
        .map(this::mapSingle);
  }

  @Override
  public Future<Transaction> trashed(Integer req) {
    String sql = "UPDATE transactions SET deleted_at = CURRENT_TIMESTAMP WHERE transaction_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(req)).map(this::mapSingle);
  }

  @Override
  public Future<Transaction> restoreTransaction(Integer req) {
    String sql = "UPDATE transactions SET deleted_at = NULL WHERE transaction_id = $1 AND deleted_at IS NOT NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(req)).map(this::mapSingle);
  }

  public Future<Boolean> deletePermanently(Integer transactionId) {
    return pool
        .preparedQuery("DELETE FROM transactions WHERE transaction_id = $1 AND deleted_at IS NOT NULL")
        .execute(Tuple.of(transactionId))
        .map(result -> result.rowCount() > 0);
  }

  public Future<Integer> restoreAllTransactions() {
    return pool
        .query("UPDATE transactions SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
        .execute()
        .map(RowSet::rowCount);
  }

  public Future<Integer> deleteAllPermanentTransactions() {
    return pool
        .query("DELETE FROM transactions WHERE deleted_at IS NOT NULL")
        .execute()
        .map(RowSet::rowCount);
  }

  private Transaction mapSingle(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Transaction.fromRow(rows.iterator().next()) : null;
  }
}
