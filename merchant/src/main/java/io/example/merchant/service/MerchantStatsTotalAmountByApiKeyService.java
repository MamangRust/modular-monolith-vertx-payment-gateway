package io.example.merchant.service;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindYearMerchantByApikey;

public interface MerchantStatsTotalAmountByApiKeyService {
  Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmounts(FindYearMerchantByApikey req);
  Future<List<MerchantStats.YearAmount>> getYearlyTotalAmounts(FindYearMerchantByApikey req);
}
