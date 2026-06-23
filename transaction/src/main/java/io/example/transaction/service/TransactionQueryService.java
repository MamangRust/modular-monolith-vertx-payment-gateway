package io.example.transaction.service;

import java.util.List;
import io.example.common.domain.ApiResponse;
import io.example.common.domain.ApiResponsePagination;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.vertx.core.Future;
import pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest;
import pb.transaction.TransactionQuery.FindAllTransactionRequest;

public interface TransactionQueryService {
  Future<ApiResponsePagination<List<TransactionResponse>>> getTransactions(FindAllTransactionRequest req);

  Future<ApiResponsePagination<List<TransactionResponseDeleteAt>>> getActiveTransactions(FindAllTransactionRequest req);

  Future<ApiResponsePagination<List<TransactionResponseDeleteAt>>> getTrashedTransactions(
      FindAllTransactionRequest req);

  Future<ApiResponse<TransactionResponse>> getTransactionById(Integer transactionId);

  Future<ApiResponsePagination<List<TransactionResponse>>> getTransactionsByCardNumber(
      FindAllTransactionCardNumberRequest req);

  Future<ApiResponse<List<TransactionResponse>>> getTransactionsByMerchantId(int merchantId);
}
