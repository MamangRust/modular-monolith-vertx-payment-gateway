package io.example.transfer.service;

import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsByCardStatusService {
  Future<List<TransferStats.MonthStatus>> getMonthlyStatusByCard(
      pb.transfer.Transfer.FindMonthlyTransferStatusCardNumber req, String status);

  Future<List<TransferStats.YearStatus>> getYearlyStatusByCard(
      pb.transfer.Transfer.FindYearTransferStatusCardNumber req, String status);
}
