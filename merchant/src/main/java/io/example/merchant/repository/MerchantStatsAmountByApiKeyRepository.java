package io.example.merchant.repository;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsAmountByApiKeyRepository {
  Future<List<MerchantStats.MonthAmount>> getMonthlyAmountByApikey(pb.merchant.Merchant.FindYearMerchantByApikey req);
  Future<List<MerchantStats.YearAmount>> getYearlyAmountByApikey(pb.merchant.Merchant.FindYearMerchantByApikey req);
}
