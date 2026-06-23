package io.example.merchant.service;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearTotalAmountApiKey;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsTotalAmountByApiKeyService {
  Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmounts(MonthYearTotalAmountApiKey req);

  Future<List<MerchantStats.YearAmount>> getYearlyTotalAmounts(MonthYearTotalAmountApiKey req);
}
