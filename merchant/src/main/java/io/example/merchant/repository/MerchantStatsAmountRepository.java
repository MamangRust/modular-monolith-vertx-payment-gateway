package io.example.merchant.repository;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.vertx.core.Future;

public interface MerchantStatsAmountRepository {
  Future<List<MerchantStats.MonthAmount>> getMonthlyAmountMerchant(int year);
  Future<List<MerchantStats.YearAmount>> getYearlyAmountMerchant(int year);
}
