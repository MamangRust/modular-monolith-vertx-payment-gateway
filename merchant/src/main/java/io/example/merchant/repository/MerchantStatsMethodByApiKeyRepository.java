package io.example.merchant.repository;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsMethodByApiKeyRepository {
  Future<List<MerchantStats.MonthMethod>> getMonthlyPaymentMethodByApikey(pb.merchant.Merchant.FindYearMerchantByApikey req);
  Future<List<MerchantStats.YearMethod>> getYearlyPaymentMethodByApikey(pb.merchant.Merchant.FindYearMerchantByApikey req);
}
