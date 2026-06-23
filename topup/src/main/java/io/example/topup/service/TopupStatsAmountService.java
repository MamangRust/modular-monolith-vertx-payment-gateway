package io.example.topup.service;

import java.util.List;

import io.example.topup.domain.requests.topup.YearTopupCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupRequest;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;

public interface TopupStatsAmountService {
  Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmounts(YearTopupRequest req);

  Future<List<TopupStats.YearAmount>> getYearlyTopupAmounts(YearTopupRequest req);

  Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmountsByCard(YearTopupCardNumberRequest req);

  Future<List<TopupStats.YearAmount>> getYearlyTopupAmountsByCard(YearTopupCardNumberRequest req);
}