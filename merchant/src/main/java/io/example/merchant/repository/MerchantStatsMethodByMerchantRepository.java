package io.example.merchant.repository;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearPaymentMethodMerchant;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsMethodByMerchantRepository {
  Future<List<MerchantStats.MonthMethod>> getMonthlyPaymentMethodByMerchants(MonthYearPaymentMethodMerchant req);

  Future<List<MerchantStats.YearMethod>> getYearlyPaymentMethodByMerchants(MonthYearPaymentMethodMerchant req);
}
