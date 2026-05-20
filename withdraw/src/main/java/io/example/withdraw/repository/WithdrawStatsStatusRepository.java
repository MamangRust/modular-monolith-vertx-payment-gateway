package io.example.withdraw.repository;

import java.util.List;
import io.example.withdraw.model.WithdrawStats;
import io.vertx.core.Future;

public interface WithdrawStatsStatusRepository {
  Future<List<WithdrawStats.MonthStatus>> getMonthlyWithdrawStatus(int year, int month, String status);

  Future<List<WithdrawStats.YearStatus>> getYearlyWithdrawStatus(int endYear, String status);

  Future<List<WithdrawStats.MonthStatus>> getMonthlyStatusByCard(String card, int year, int month, String status);

  Future<List<WithdrawStats.YearStatus>> getYearlyStatusByCard(String card, int year, String status);
}
