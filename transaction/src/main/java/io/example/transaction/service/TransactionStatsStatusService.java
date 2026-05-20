package io.example.transaction.service;

import java.util.List;
import io.example.transaction.model.TransactionStats;
import io.vertx.core.Future;

import pb.transaction.Transaction.FindMonthlyTransactionStatus;
import pb.transaction.Transaction.FindYearTransactionStatus;
import pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber;
import pb.transaction.Transaction.FindYearTransactionStatusCardNumber;

public interface TransactionStatsStatusService {
  Future<List<TransactionStats.MonthStatus>> getMonthlyStatus(FindMonthlyTransactionStatus request, String status);
  Future<List<TransactionStats.YearStatus>> getYearlyStatus(FindYearTransactionStatus request, String status);
  Future<List<TransactionStats.MonthStatus>> getMonthlyStatusByCard(FindMonthlyTransactionStatusCardNumber request, String status);
  Future<List<TransactionStats.YearStatus>> getYearlyStatusByCard(FindYearTransactionStatusCardNumber request, String status);
}
