package io.example.merchant.repository;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsMethodRepository {
  Future<List<MerchantStats.MonthMethod>> getMonthlyPaymentMethodsMerchant(int year);
  Future<List<MerchantStats.YearMethod>> getYearlyPaymentMethodMerchant(int year);
}
