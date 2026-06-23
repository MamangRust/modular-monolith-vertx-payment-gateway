package io.example.transaction.service;

import java.util.List;

import io.example.transaction.domain.requests.YearCardNumberTransactionRequest;
import io.example.transaction.domain.requests.YearTransactionRequest;
import io.example.transaction.model.TransactionStats;
import io.vertx.core.Future;

public interface TransactionStatsMethodService {
  Future<List<TransactionStats.MonthMethod>> getMonthlyMethods(YearTransactionRequest req);

  Future<List<TransactionStats.YearMethod>> getYearlyMethods(YearTransactionRequest req);

  Future<List<TransactionStats.MonthMethod>> getMonthlyMethodsByCard(YearCardNumberTransactionRequest req);

  Future<List<TransactionStats.YearMethod>> getYearlyMethodsByCard(YearCardNumberTransactionRequest req);
}