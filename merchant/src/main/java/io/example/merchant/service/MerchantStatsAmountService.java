package io.example.merchant.service;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindYearMerchant;

public interface MerchantStatsAmountService {
  Future<List<MerchantStats.MonthAmount>> getMonthlyAmounts(FindYearMerchant req);
  Future<List<MerchantStats.YearAmount>> getYearlyAmounts(FindYearMerchant req);
}
