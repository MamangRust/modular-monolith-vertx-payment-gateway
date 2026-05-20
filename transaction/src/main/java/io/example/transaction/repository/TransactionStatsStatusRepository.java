package io.example.transaction.repository;

import java.util.List;
import io.example.transaction.model.TransactionStats;
import io.vertx.core.Future;

import pb.transaction.Transaction.FindMonthlyTransactionStatus;
import pb.transaction.Transaction.FindYearTransactionStatus;
import pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber;
import pb.transaction.Transaction.FindYearTransactionStatusCardNumber;

public interface TransactionStatsStatusRepository {
  Future<List<TransactionStats.MonthStatus>> getMonthlyStatus(FindMonthlyTransactionStatus req, String status);
  Future<List<TransactionStats.YearStatus>> getYearlyStatus(FindYearTransactionStatus req, String status);
  Future<List<TransactionStats.MonthStatus>> getMonthlyStatusByCard(FindMonthlyTransactionStatusCardNumber req, String status);
  Future<List<TransactionStats.YearStatus>> getYearlyStatusByCard(FindYearTransactionStatusCardNumber req, String status);
}
