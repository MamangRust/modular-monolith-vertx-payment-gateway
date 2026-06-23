package io.example.merchant.service;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearPaymentMethodApiKey;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsMethodByApiKeyService {
  Future<List<MerchantStats.MonthMethod>> getMonthlyMethodAmounts(MonthYearPaymentMethodApiKey req);

  Future<List<MerchantStats.YearMethod>> getYearlyMethodAmounts(MonthYearPaymentMethodApiKey req);
}
