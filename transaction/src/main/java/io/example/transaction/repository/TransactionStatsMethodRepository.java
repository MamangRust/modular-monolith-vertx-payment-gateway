package io.example.transaction.repository;

import java.util.List;
import io.example.transaction.model.TransactionStats;
import io.vertx.core.Future;

import pb.transaction.Transaction.FindYearTransactionStatus;
import pb.transaction.Transaction.FindByYearCardNumberTransactionRequest;

public interface TransactionStatsMethodRepository {
  Future<List<TransactionStats.MonthMethod>> getMonthlyMethods(FindYearTransactionStatus request);
  Future<List<TransactionStats.YearMethod>> getYearlyMethods(FindYearTransactionStatus request);
  Future<List<TransactionStats.MonthMethod>> getMonthlyMethodsByCard(FindByYearCardNumberTransactionRequest request);
  Future<List<TransactionStats.YearMethod>> getYearlyMethodsByCard(FindByYearCardNumberTransactionRequest request);
}
