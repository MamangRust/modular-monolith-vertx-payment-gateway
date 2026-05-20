package io.example.transaction.service;

import io.example.common.model.ApiResponse;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.vertx.core.Future;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;

public interface TransactionCommandService {
  Future<ApiResponse<TransactionResponse>> createTransaction(CreateTransactionRequest req);
  Future<ApiResponse<TransactionResponse>> updateTransaction(UpdateTransactionRequest req);
  Future<ApiResponse<TransactionResponseDeleteAt>> trashTransaction(Integer transactionId);
  Future<ApiResponse<TransactionResponseDeleteAt>> restoreTransaction(Integer transactionId);
  Future<ApiResponse<Void>> deleteTransactionPermanently(Integer transactionId);
  Future<ApiResponse<Void>> restoreAllTransactions();
  Future<ApiResponse<Void>> deleteAllPermanentTransactions();
}
