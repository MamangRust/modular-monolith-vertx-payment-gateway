package io.example.topup.repository;

import java.util.List;

import io.example.topup.domain.requests.topup.YearTopupCardNumberRequest;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;

public interface TopupStatsByCardMethodRepository {
  Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethodsByCard(YearTopupCardNumberRequest req);

  Future<List<TopupStats.YearMethod>> getYearlyTopupMethodsByCard(YearTopupCardNumberRequest req);
}
