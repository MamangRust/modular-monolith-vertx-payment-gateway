package io.example.merchant.repository;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearAmountApiKey;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsAmountByApiKeyRepository {
  Future<List<MerchantStats.MonthAmount>> getMonthlyAmountByApikey(MonthYearAmountApiKey req);

  Future<List<MerchantStats.YearAmount>> getYearlyAmountByApikey(MonthYearAmountApiKey req);
}
