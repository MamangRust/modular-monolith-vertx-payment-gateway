package io.example.topup.repository;

import java.util.List;

import io.example.topup.domain.requests.topup.YearTopupRequest;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;

public interface TopupStatsAmountRepository {
  Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmounts(YearTopupRequest req);

  Future<List<TopupStats.YearAmount>> getYearlyTopupAmounts(YearTopupRequest req);
}
