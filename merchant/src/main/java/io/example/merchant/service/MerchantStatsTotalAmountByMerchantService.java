package io.example.merchant.service;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindYearMerchantById;

public interface MerchantStatsTotalAmountByMerchantService {
  Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmounts(FindYearMerchantById req);
  Future<List<MerchantStats.YearAmount>> getYearlyTotalAmounts(FindYearMerchantById req);
}
