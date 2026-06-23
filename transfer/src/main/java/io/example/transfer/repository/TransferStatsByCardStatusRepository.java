package io.example.transfer.repository;

import java.util.List;

import io.example.transfer.domain.requests.MonthStatusTransferCardNumber;
import io.example.transfer.domain.requests.YearStatusTransferCardNumber;
import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;

public interface TransferStatsByCardStatusRepository {
  Future<List<TransferStats.MonthStatus>> getMonthlyStatusByCard(MonthStatusTransferCardNumber req);

  Future<List<TransferStats.YearStatus>> getYearlyStatusByCard(YearStatusTransferCardNumber req);
}
