package io.example.transfer.repository;

import java.util.List;

import io.example.transfer.domain.requests.MonthYearCardNumber;
import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;

public interface TransferStatsByCardAmountReceiverRepository {
  Future<List<TransferStats.MonthAmount>> getMonthlyReceiverAmountsByCard(MonthYearCardNumber req);

  Future<List<TransferStats.YearAmount>> getYearlyReceiverAmountsByCard(MonthYearCardNumber req);
}
