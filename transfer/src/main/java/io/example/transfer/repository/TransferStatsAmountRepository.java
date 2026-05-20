package io.example.transfer.repository;

import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsAmountRepository {
  Future<List<TransferStats.MonthAmount>> getMonthlyTransferAmounts(int year);

  Future<List<TransferStats.YearAmount>> getYearlyTransferAmounts(int endYear);
}
