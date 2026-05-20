package io.example.transaction.repository;

import java.util.List;
import io.example.transaction.model.TransactionStats;
import io.vertx.core.Future;

import pb.transaction.Transaction.FindYearTransactionStatus;
import pb.transaction.Transaction.FindByYearCardNumberTransactionRequest;

public interface TransactionStatsAmountRepository {
  Future<List<TransactionStats.MonthAmount>> getMonthlyAmounts(FindYearTransactionStatus request);
  Future<List<TransactionStats.YearAmount>> getYearlyAmounts(FindYearTransactionStatus request);
  Future<List<TransactionStats.MonthAmount>> getMonthlyAmountsByCard(FindByYearCardNumberTransactionRequest request);
  Future<List<TransactionStats.YearAmount>> getYearlyAmountsByCard(FindByYearCardNumberTransactionRequest request);
}
