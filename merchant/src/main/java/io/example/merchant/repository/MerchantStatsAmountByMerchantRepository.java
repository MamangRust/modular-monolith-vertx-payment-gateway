package io.example.merchant.repository;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsAmountByMerchantRepository {
  Future<List<MerchantStats.MonthAmount>> getMonthlyAmountByMerchants(pb.merchant.Merchant.FindYearMerchantById req);
  Future<List<MerchantStats.YearAmount>> getYearlyAmountByMerchants(pb.merchant.Merchant.FindYearMerchantById req);
}
