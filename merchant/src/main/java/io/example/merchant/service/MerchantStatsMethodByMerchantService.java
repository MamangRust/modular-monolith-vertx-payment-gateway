package io.example.merchant.service;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindYearMerchantById;

public interface MerchantStatsMethodByMerchantService {
  Future<List<MerchantStats.MonthMethod>> getMonthlyMethodAmounts(FindYearMerchantById req);
  Future<List<MerchantStats.YearMethod>> getYearlyMethodAmounts(FindYearMerchantById req);
}
