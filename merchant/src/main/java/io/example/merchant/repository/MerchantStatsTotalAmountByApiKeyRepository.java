package io.example.merchant.repository;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsTotalAmountByApiKeyRepository {
  Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmountByApikey(pb.merchant.Merchant.FindYearMerchantByApikey req);
  Future<List<MerchantStats.YearAmount>> getYearlyTotalAmountByApikey(pb.merchant.Merchant.FindYearMerchantByApikey req);
}
