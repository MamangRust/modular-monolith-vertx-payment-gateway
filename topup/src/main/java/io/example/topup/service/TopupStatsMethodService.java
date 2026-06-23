package io.example.topup.service;

import java.util.List;

import io.example.topup.domain.requests.topup.YearTopupCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupRequest;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;

public interface TopupStatsMethodService {
  Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethods(YearTopupRequest req);

  Future<List<TopupStats.YearMethod>> getYearlyTopupMethods(YearTopupRequest req);

  Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethodsByCard(YearTopupCardNumberRequest req);

  Future<List<TopupStats.YearMethod>> getYearlyTopupMethodsByCard(YearTopupCardNumberRequest req);
}