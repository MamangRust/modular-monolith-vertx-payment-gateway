package io.example.topup.repository;

import java.util.List;

import io.example.topup.domain.requests.topup.MonthTopupStatusCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusCardNumberRequest;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;

public interface TopupStatsByCardStatusRepository {
  Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatusByCard(MonthTopupStatusCardNumberRequest req);

  Future<List<TopupStats.YearStatus>> getYearlyTopupStatusByCard(YearTopupStatusCardNumberRequest req);
}
