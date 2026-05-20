package io.example.merchant.service;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindYearMerchantByApikey;

public interface MerchantStatsMethodByApiKeyService {
  Future<List<MerchantStats.MonthMethod>> getMonthlyMethodAmounts(FindYearMerchantByApikey req);
  Future<List<MerchantStats.YearMethod>> getYearlyMethodAmounts(FindYearMerchantByApikey req);
}
