package io.example.transaction.repository;

import java.util.List;
import io.example.transaction.model.TransactionStats;
import io.vertx.core.Future;

import io.example.transaction.domain.requests.YearCardNumberTransactionRequest;
import io.example.transaction.domain.requests.YearTransactionRequest;

public interface TransactionStatsAmountRepository {
  Future<List<TransactionStats.MonthAmount>> getMonthlyAmounts(YearTransactionRequest request);

  Future<List<TransactionStats.YearAmount>> getYearlyAmounts(YearTransactionRequest request);

  Future<List<TransactionStats.MonthAmount>> getMonthlyAmountsByCard(YearCardNumberTransactionRequest request);

  Future<List<TransactionStats.YearAmount>> getYearlyAmountsByCard(YearCardNumberTransactionRequest request);
}
