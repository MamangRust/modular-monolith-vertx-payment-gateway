package io.example.transfer.repository;

import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsByCardAmountReceiverRepository {
  Future<List<TransferStats.MonthAmount>> getMonthlyReceiverAmountsByCard(String card, int year);
  Future<List<TransferStats.YearAmount>> getYearlyReceiverAmountsByCard(String card, int year);
}
