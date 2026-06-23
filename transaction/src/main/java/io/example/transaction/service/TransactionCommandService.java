package io.example.transaction.service;

import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.vertx.core.Future;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;

public interface TransactionCommandService {
  Future<TransactionResponse> createTransaction(CreateTransactionRequest req);

  Future<TransactionResponse> updateTransaction(UpdateTransactionRequest req);

  Future<TransactionResponseDeleteAt> trashTransaction(Integer transactionId);

  Future<TransactionResponseDeleteAt> restoreTransaction(Integer transactionId);

  Future<Void> deleteTransactionPermanently(Integer transactionId);

  Future<Void> restoreAllTransactions();

  Future<Void> deleteAllPermanentTransactions();
}