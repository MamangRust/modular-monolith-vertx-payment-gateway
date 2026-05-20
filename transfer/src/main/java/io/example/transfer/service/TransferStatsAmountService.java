package io.example.transfer.service;

import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsAmountService {
  Future<List<TransferStats.MonthAmount>> getMonthlyTransferAmounts(int year);

  Future<List<TransferStats.YearAmount>> getYearlyTransferAmounts(int endYear);
}
