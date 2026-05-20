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
import pb.transaction.Transaction.FindByIdTransactionRequest;

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
  public Future<Transaction> updateTransactionStatus(int id, String status) {
    String sql = "UPDATE transactions SET status = $2, updated_at = CURRENT_TIMESTAMP WHERE transaction_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(id, status)).map(this::mapSingle);
  }

  @Override
  public Future<Transaction> trashTransaction(FindByIdTransactionRequest req) {
    String sql = "UPDATE transactions SET deleted_at = CURRENT_TIMESTAMP WHERE transaction_id = $1 AND deleted_at IS NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of(req.getTransactionId())).map(this::mapSingle);
  }

  @Override
  public Future<Transaction> getTransactionById(FindByIdTransactionRequest req) {
    String sql = "SELECT * FROM transactions WHERE transaction_id = $1 AND deleted_at IS NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(req.getTransactionId()))
        .map(this::mapSingle);
  }

  @Override
  public Future<Transaction> getTrashedTransactionById(FindByIdTransactionRequest req) {
    String sql = "SELECT * FROM transactions WHERE transaction_id = $1 AND deleted_at IS NOT NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(req.getTransactionId()))
        .map(this::mapSingle);
  }

  @Override
  public Future<Void> deleteTransaction(FindByIdTransactionRequest req) {
    String sql = "UPDATE transactions SET deleted_at = NOW() WHERE transaction_id = $1";
    return pool.preparedQuery(sql).execute(Tuple.of(req.getTransactionId()))
        .mapEmpty();
  }

  @Override
  public Future<Transaction> restoreTransaction(FindByIdTransactionRequest req) {
    String sql = "UPDATE transactions SET deleted_at = NULL WHERE transaction_id = $1 AND deleted_at IS NOT NULL RETURNING *";
    return pool.preparedQuery(sql).execute(Tuple.of((int) req.getTransactionId())).map(this::mapSingle);
  }

  @Override
  public Future<Void> deletePermanently(FindByIdTransactionRequest req) {
    return pool.preparedQuery("DELETE FROM transactions WHERE transaction_id = $1").execute(Tuple.of((int) req.getTransactionId())).mapEmpty();
  }

  @Override
  public Future<Void> restoreAll() {
    return pool.query("UPDATE transactions SET deleted_at = NULL WHERE deleted_at IS NOT NULL").execute().mapEmpty();
  }

  @Override
  public Future<Void> deleteAllPermanent() {
    return pool.query("DELETE FROM transactions WHERE deleted_at IS NOT NULL").execute().mapEmpty();
  }

  private Transaction mapSingle(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Transaction.fromRow(rows.iterator().next()) : null;
  }
}
