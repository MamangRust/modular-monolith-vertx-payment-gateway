package io.example.merchant.service;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindYearMerchant;

public interface MerchantStatsTotalAmountService {
  Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmounts(FindYearMerchant req);
  Future<List<MerchantStats.YearAmount>> getYearlyTotalAmounts(FindYearMerchant req);
}
