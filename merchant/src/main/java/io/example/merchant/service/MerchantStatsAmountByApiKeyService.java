package io.example.merchant.service;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindYearMerchantByApikey;

public interface MerchantStatsAmountByApiKeyService {
  Future<List<MerchantStats.MonthAmount>> getMonthlyAmounts(FindYearMerchantByApikey req);
  Future<List<MerchantStats.YearAmount>> getYearlyAmounts(FindYearMerchantByApikey req);
}
