package io.example.topup.repository;

import java.util.List;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;
import pb.topup.Topup.FindYearTopupStatus;

public interface TopupStatsMethodRepository {
  Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethods(FindYearTopupStatus req);
  Future<List<TopupStats.YearMethod>> getYearlyTopupMethods(FindYearTopupStatus req);
}
