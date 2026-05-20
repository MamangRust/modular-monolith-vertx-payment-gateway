package io.example.transfer.repository;

import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsByCardAmountSenderRepository {
  Future<List<TransferStats.MonthAmount>> getMonthlySenderAmountsByCard(String card, int year);
  Future<List<TransferStats.YearAmount>> getYearlySenderAmountsByCard(String card, int year);
}
