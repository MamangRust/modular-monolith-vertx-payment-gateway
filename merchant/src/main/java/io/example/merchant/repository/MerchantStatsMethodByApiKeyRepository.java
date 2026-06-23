package io.example.merchant.repository;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearPaymentMethodApiKey;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsMethodByApiKeyRepository {
  Future<List<MerchantStats.MonthMethod>> getMonthlyPaymentMethodByApikey(MonthYearPaymentMethodApiKey req);

  Future<List<MerchantStats.YearMethod>> getYearlyPaymentMethodByApikey(MonthYearPaymentMethodApiKey req);
}
