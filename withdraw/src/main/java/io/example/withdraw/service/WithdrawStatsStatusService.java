package io.example.withdraw.service;

import java.util.List;

import io.example.withdraw.domain.requests.MonthStatusWithdrawCardNumber;
import io.example.withdraw.domain.requests.YearStatusWithdrawCardNumber;
import io.example.withdraw.model.WithdrawStats;
import io.vertx.core.Future;

public interface WithdrawStatsStatusService {
  Future<List<WithdrawStats.MonthStatus>> getMonthlyWithdrawStatus(MonthStatusWithdrawCardNumber req);

  Future<List<WithdrawStats.YearStatus>> getYearlyWithdrawStatus(YearStatusWithdrawCardNumber req);

  Future<List<WithdrawStats.MonthStatus>> getMonthlyStatusByCard(MonthStatusWithdrawCardNumber req);

  Future<List<WithdrawStats.YearStatus>> getYearlyStatusByCard(YearStatusWithdrawCardNumber req);
}