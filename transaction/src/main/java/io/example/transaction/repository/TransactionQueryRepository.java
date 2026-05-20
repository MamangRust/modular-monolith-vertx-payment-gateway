package io.example.transaction.repository;

import io.example.common.domain.PagedResult;
import io.example.transaction.domain.requests.FindAllTransactionCardNumber;
import io.example.transaction.domain.requests.FindAllTransactions;
import io.example.transaction.model.Transaction;
import io.vertx.core.Future;

public interface TransactionQueryRepository {
  Future<PagedResult<Transaction>> getTransactions(FindAllTransactions req);
  Future<PagedResult<Transaction>> getActiveTransactions(FindAllTransactions req);
  Future<PagedResult<Transaction>> getTrashedTransactions(FindAllTransactions req);
  Future<Transaction> getTransactionById(int transactionId);
  Future<PagedResult<Transaction>> getTransactionsByCardNumber(FindAllTransactionCardNumber req);
  Future<PagedResult<Transaction>> getTransactionsByMerchantId(int merchantId);
}
