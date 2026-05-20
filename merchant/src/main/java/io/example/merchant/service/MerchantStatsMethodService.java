package io.example.merchant.service;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindYearMerchant;

public interface MerchantStatsMethodService {
  Future<List<MerchantStats.MonthMethod>> getMonthlyMethodAmounts(FindYearMerchant req);
  Future<List<MerchantStats.YearMethod>> getYearlyMethodAmounts(FindYearMerchant req);
}
