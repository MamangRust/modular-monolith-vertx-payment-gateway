package io.example.transfer.repository;

import io.example.transfer.domain.requests.MonthYearCardNumber;
import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsByCardAmountSenderRepository {
  Future<List<TransferStats.MonthAmount>> getMonthlySenderAmountsByCard(MonthYearCardNumber req);

  Future<List<TransferStats.YearAmount>> getYearlySenderAmountsByCard(MonthYearCardNumber req);
}
