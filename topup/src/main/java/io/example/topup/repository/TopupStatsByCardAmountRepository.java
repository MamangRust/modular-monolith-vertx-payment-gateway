package io.example.topup.repository;

import java.util.List;

import io.example.topup.domain.requests.topup.YearTopupCardNumberRequest;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;

public interface TopupStatsByCardAmountRepository {
  Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmountsByCard(YearTopupCardNumberRequest req);

  Future<List<TopupStats.YearAmount>> getYearlyTopupAmountsByCard(YearTopupCardNumberRequest req);
}
