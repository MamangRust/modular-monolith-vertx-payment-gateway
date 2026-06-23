package io.example.merchant.service;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearAmountApiKey;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsAmountByApiKeyService {
  Future<List<MerchantStats.MonthAmount>> getMonthlyAmounts(MonthYearAmountApiKey req);

  Future<List<MerchantStats.YearAmount>> getYearlyAmounts(MonthYearAmountApiKey req);
}
