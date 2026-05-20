package io.example.withdraw.repository;

import java.util.List;
import io.example.withdraw.model.WithdrawStats;
import io.vertx.core.Future;

public interface WithdrawStatsAmountRepository {
  Future<List<WithdrawStats.MonthAmount>> getMonthlyWithdrawAmounts(int year);

  Future<List<WithdrawStats.YearAmount>> getYearlyWithdrawAmounts(int endYear);

  Future<List<WithdrawStats.MonthAmount>> getMonthlyWithdrawAmountsByCard(String card, int year);

  Future<List<WithdrawStats.YearAmount>> getYearlyWithdrawAmountsByCard(String card, int year);
}
