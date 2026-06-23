package io.example.merchant.service;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearPaymentMethodMerchant;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsMethodByMerchantService {
  Future<List<MerchantStats.MonthMethod>> getMonthlyMethodAmounts(MonthYearPaymentMethodMerchant req);

  Future<List<MerchantStats.YearMethod>> getYearlyMethodAmounts(MonthYearPaymentMethodMerchant req);
}
