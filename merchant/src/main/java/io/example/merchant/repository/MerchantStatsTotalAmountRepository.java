package io.example.merchant.repository;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsTotalAmountRepository {
  Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmountMerchant(int year);
  Future<List<MerchantStats.YearAmount>> getYearlyTotalAmountMerchant(int year);
}
