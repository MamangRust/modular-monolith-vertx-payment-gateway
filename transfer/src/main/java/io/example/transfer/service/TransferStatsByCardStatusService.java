package io.example.transfer.service;

import io.example.transfer.domain.requests.MonthStatusTransferCardNumber;
import io.example.transfer.domain.requests.YearStatusTransferCardNumber;
import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsByCardStatusService {
    Future<List<TransferStats.MonthStatus>> getMonthlyStatusByCard(MonthStatusTransferCardNumber req);

    Future<List<TransferStats.YearStatus>> getYearlyStatusByCard(YearStatusTransferCardNumber req);
}