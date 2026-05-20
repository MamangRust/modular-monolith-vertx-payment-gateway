package io.example.transfer.repository;

import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsByCardStatusRepository {
  Future<List<TransferStats.MonthStatus>> getMonthlyStatusByCard(pb.transfer.Transfer.FindMonthlyTransferStatusCardNumber req, String status);
  Future<List<TransferStats.YearStatus>> getYearlyStatusByCard(pb.transfer.Transfer.FindYearTransferStatusCardNumber req, String status);
}
