package io.example.merchant.repository;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsMethodByMerchantRepository {
  Future<List<MerchantStats.MonthMethod>> getMonthlyPaymentMethodByMerchants(pb.merchant.Merchant.FindYearMerchantById req);
  Future<List<MerchantStats.YearMethod>> getYearlyPaymentMethodByMerchants(pb.merchant.Merchant.FindYearMerchantById req);
}
