package io.example.merchant.repository;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearAmountMerchant;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsAmountByMerchantRepository {
  Future<List<MerchantStats.MonthAmount>> getMonthlyAmountByMerchants(MonthYearAmountMerchant req);

  Future<List<MerchantStats.YearAmount>> getYearlyAmountByMerchants(MonthYearAmountMerchant req);
}
