package io.example.withdraw.service;

import java.util.List;

import io.example.withdraw.domain.requests.YearMonthCardNumber;
import io.example.withdraw.model.WithdrawStats;
import io.vertx.core.Future;

public interface WithdrawStatsAmountService {
  Future<List<WithdrawStats.MonthAmount>> getMonthlyWithdrawAmounts(int year);

  Future<List<WithdrawStats.YearAmount>> getYearlyWithdrawAmounts(int year);

  Future<List<WithdrawStats.MonthAmount>> getMonthlyWithdrawAmountsByCard(YearMonthCardNumber req);

  Future<List<WithdrawStats.YearAmount>> getYearlyWithdrawAmountsByCard(YearMonthCardNumber req);
}