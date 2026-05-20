package io.example.topup.service;

import java.util.List;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;
import pb.topup.Topup.FindYearTopupCardNumber;
import pb.topup.Topup.FindYearTopupStatus;

public interface TopupStatsAmountService {
  Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmounts(FindYearTopupStatus req);
  Future<List<TopupStats.YearAmount>> getYearlyTopupAmounts(FindYearTopupStatus req);
  Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmountsByCard(FindYearTopupCardNumber req);
  Future<List<TopupStats.YearAmount>> getYearlyTopupAmountsByCard(FindYearTopupCardNumber req);
}
