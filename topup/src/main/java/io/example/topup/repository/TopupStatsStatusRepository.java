package io.example.topup.repository;

import java.util.List;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;
import pb.topup.Topup.FindMonthlyTopupStatus;
import pb.topup.Topup.FindYearTopupStatus;

public interface TopupStatsStatusRepository {
  Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatus(FindMonthlyTopupStatus req, String status);
  Future<List<TopupStats.YearStatus>> getYearlyTopupStatus(FindYearTopupStatus req, String status);
}
