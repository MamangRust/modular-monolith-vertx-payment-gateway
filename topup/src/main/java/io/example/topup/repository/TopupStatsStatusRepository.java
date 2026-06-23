package io.example.topup.repository;

import java.util.List;

import io.example.topup.domain.requests.topup.MonthTopupStatusRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusRequest;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;

public interface TopupStatsStatusRepository {
  Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatus(MonthTopupStatusRequest req);

  Future<List<TopupStats.YearStatus>> getYearlyTopupStatus(YearTopupStatusRequest req);
}
