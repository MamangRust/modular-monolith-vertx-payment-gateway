package io.example.withdraw.service;

import java.util.List;
import io.example.withdraw.model.WithdrawStats;
import io.vertx.core.Future;
import pb.withdraw.Withdraw.FindYearWithdrawStatus;
import pb.withdraw.Withdraw.FindYearWithdrawCardNumber;

public interface WithdrawStatsAmountService {
  Future<List<WithdrawStats.MonthAmount>> getMonthlyWithdrawAmounts(FindYearWithdrawStatus req);

  Future<List<WithdrawStats.YearAmount>> getYearlyWithdrawAmounts(FindYearWithdrawStatus req);

  Future<List<WithdrawStats.MonthAmount>> getMonthlyWithdrawAmountsByCard(FindYearWithdrawCardNumber req);

  Future<List<WithdrawStats.YearAmount>> getYearlyWithdrawAmountsByCard(FindYearWithdrawCardNumber req);
}
