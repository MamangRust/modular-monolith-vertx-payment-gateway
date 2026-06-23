package io.example.merchant.service;

import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearTotalAmountMerchant;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsTotalAmountByMerchantService {
  Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmounts(MonthYearTotalAmountMerchant req);

  Future<List<MerchantStats.YearAmount>> getYearlyTotalAmounts(MonthYearTotalAmountMerchant req);
}
