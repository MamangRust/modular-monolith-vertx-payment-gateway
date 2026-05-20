package io.example.topup.repository;

import java.util.List;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;
import pb.topup.Topup.FindYearTopupStatus;

public interface TopupStatsAmountRepository {
  Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmounts(FindYearTopupStatus req);
  Future<List<TopupStats.YearAmount>> getYearlyTopupAmounts(FindYearTopupStatus req);
}
