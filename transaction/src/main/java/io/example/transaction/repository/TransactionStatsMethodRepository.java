package io.example.transaction.repository;

import java.util.List;
import io.example.transaction.model.TransactionStats;
import io.vertx.core.Future;

import io.example.transaction.domain.requests.YearCardNumberTransactionRequest;
import io.example.transaction.domain.requests.YearTransactionRequest;

public interface TransactionStatsMethodRepository {
  Future<List<TransactionStats.MonthMethod>> getMonthlyMethods(YearTransactionRequest request);

  Future<List<TransactionStats.YearMethod>> getYearlyMethods(YearTransactionRequest request);

  Future<List<TransactionStats.MonthMethod>> getMonthlyMethodsByCard(YearCardNumberTransactionRequest request);

  Future<List<TransactionStats.YearMethod>> getYearlyMethodsByCard(YearCardNumberTransactionRequest request);
}
