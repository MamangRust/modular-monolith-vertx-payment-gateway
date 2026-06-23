package io.example.transaction.repository;

import io.example.transaction.model.Transaction;
import io.vertx.core.Future;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;

public interface TransactionCommandRepository {
  Future<Transaction> createTransaction(CreateTransactionRequest req);

  Future<Transaction> updateTransaction(UpdateTransactionRequest req);

  Future<Transaction> updateTransactionStatus(Integer id, String status);

  Future<Transaction> getTransactionById(Integer req);

  Future<Transaction> getTrashedTransactionById(Integer req);

  Future<Transaction> trashed(Integer req);

  Future<Transaction> restoreTransaction(Integer id);

  Future<Boolean> deletePermanently(Integer transactionId);

  Future<Integer> restoreAllTransactions();

  Future<Integer> deleteAllPermanentTransactions();
}
