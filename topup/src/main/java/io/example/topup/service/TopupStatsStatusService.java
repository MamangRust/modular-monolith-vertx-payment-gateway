package io.example.topup.service;

import java.util.List;

import io.example.topup.domain.requests.topup.MonthTopupStatusCardNumberRequest;
import io.example.topup.domain.requests.topup.MonthTopupStatusRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusRequest;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;

public interface TopupStatsStatusService {
  Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatus(MonthTopupStatusRequest req);

  Future<List<TopupStats.YearStatus>> getYearlyTopupStatus(YearTopupStatusRequest req);

  Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatusByCard(MonthTopupStatusCardNumberRequest req);

  Future<List<TopupStats.YearStatus>> getYearlyTopupStatusByCard(YearTopupStatusCardNumberRequest req);
}