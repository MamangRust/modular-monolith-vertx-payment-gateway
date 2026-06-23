package io.example.transaction.service;

import java.util.List;

import io.example.transaction.domain.requests.YearCardNumberTransactionRequest;
import io.example.transaction.domain.requests.YearTransactionRequest;
import io.example.transaction.model.TransactionStats;
import io.vertx.core.Future;

public interface TransactionStatsAmountService {
  Future<List<TransactionStats.MonthAmount>> getMonthlyAmounts(YearTransactionRequest req);

  Future<List<TransactionStats.YearAmount>> getYearlyAmounts(YearTransactionRequest req);

  Future<List<TransactionStats.MonthAmount>> getMonthlyAmountsByCard(YearCardNumberTransactionRequest req);

  Future<List<TransactionStats.YearAmount>> getYearlyAmountsByCard(YearCardNumberTransactionRequest req);
}