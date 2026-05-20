package io.example.transaction.repository;

import io.example.transaction.model.Transaction;
import io.vertx.core.Future;

import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;
import pb.transaction.Transaction.FindByIdTransactionRequest;

public interface TransactionCommandRepository {
  Future<Transaction> createTransaction(CreateTransactionRequest req);
  Future<Transaction> updateTransaction(UpdateTransactionRequest req);
  Future<Transaction> updateTransactionStatus(int id, String status);
  Future<Transaction> trashTransaction(FindByIdTransactionRequest req);
  Future<Transaction> getTransactionById(FindByIdTransactionRequest req);
  Future<Transaction> getTrashedTransactionById(FindByIdTransactionRequest req);
  Future<Void> deleteTransaction(FindByIdTransactionRequest req);
  Future<Transaction> restoreTransaction(FindByIdTransactionRequest req);
  Future<Void> deletePermanently(FindByIdTransactionRequest req);
  Future<Void> restoreAll();
  Future<Void> deleteAllPermanent();
}
