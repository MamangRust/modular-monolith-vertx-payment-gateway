package io.example.transfer.service;

import io.example.transfer.domain.requests.MonthYearCardNumber;
import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsByCardAmountService {
  Future<List<TransferStats.MonthAmount>> getMonthlySenderAmountsByCard(MonthYearCardNumber req);

  Future<List<TransferStats.MonthAmount>> getMonthlyReceiverAmountsByCard(MonthYearCardNumber req);

  Future<List<TransferStats.YearAmount>> getYearlySenderAmountsByCard(MonthYearCardNumber req);

  Future<List<TransferStats.YearAmount>> getYearlyReceiverAmountsByCard(MonthYearCardNumber req);
}