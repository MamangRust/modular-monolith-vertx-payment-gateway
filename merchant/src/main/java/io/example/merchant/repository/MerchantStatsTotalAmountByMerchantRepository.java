package io.example.merchant.repository;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsTotalAmountByMerchantRepository {
  Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmountByMerchants(pb.merchant.Merchant.FindYearMerchantById req);
  Future<List<MerchantStats.YearAmount>> getYearlyTotalAmountByMerchants(pb.merchant.Merchant.FindYearMerchantById req);
}
