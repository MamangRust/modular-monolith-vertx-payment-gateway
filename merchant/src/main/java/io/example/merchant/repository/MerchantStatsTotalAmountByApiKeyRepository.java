package io.example.merchant.repository;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearTotalAmountApiKey;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsTotalAmountByApiKeyRepository {
  Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmountByApikey(MonthYearTotalAmountApiKey req);

  Future<List<MerchantStats.YearAmount>> getYearlyTotalAmountByApikey(MonthYearTotalAmountApiKey req);
}
