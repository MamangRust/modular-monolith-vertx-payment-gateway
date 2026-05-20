package io.example.transfer.service;

import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsByCardAmountService {
  Future<List<TransferStats.MonthAmount>> getMonthlySenderAmountsByCard(String card, int year);
  Future<List<TransferStats.MonthAmount>> getMonthlyReceiverAmountsByCard(String card, int year);
  Future<List<TransferStats.YearAmount>> getYearlySenderAmountsByCard(String card, int year);
  Future<List<TransferStats.YearAmount>> getYearlyReceiverAmountsByCard(String card, int year);
}
