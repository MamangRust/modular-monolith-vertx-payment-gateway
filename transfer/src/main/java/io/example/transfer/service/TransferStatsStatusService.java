package io.example.transfer.service;

import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsStatusService {
  Future<List<TransferStats.MonthStatus>> getMonthlyTransferStatus(
      pb.transfer.Transfer.FindMonthlyTransferStatus req, String status);

  Future<List<TransferStats.YearStatus>> getYearlyTransferStatus(
      pb.transfer.Transfer.FindYearTransferStatus req, String status);
}
