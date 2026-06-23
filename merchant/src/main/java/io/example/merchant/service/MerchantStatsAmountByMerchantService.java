package io.example.merchant.service;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearAmountMerchant;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsAmountByMerchantService {
  Future<List<MerchantStats.MonthAmount>> getMonthlyAmounts(MonthYearAmountMerchant req);

  Future<List<MerchantStats.YearAmount>> getYearlyAmounts(MonthYearAmountMerchant req);
}
