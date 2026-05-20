package io.example.merchant.service;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindYearMerchantById;

public interface MerchantStatsAmountByMerchantService {
  Future<List<MerchantStats.MonthAmount>> getMonthlyAmounts(FindYearMerchantById req);
  Future<List<MerchantStats.YearAmount>> getYearlyAmounts(FindYearMerchantById req);
}
