package io.example.merchant.repository;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearTotalAmountMerchant;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsTotalAmountByMerchantRepository {
  Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmountByMerchants(MonthYearTotalAmountMerchant req);

  Future<List<MerchantStats.YearAmount>> getYearlyTotalAmountByMerchants(MonthYearTotalAmountMerchant req);
}
