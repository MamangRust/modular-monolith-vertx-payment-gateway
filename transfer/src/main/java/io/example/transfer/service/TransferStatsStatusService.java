package io.example.transfer.service;

import io.example.transfer.domain.requests.MonthStatusTransfer;
import io.example.transfer.domain.requests.YearStatusTransferRequest;
import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;
import java.util.List;

public interface TransferStatsStatusService {
    Future<List<TransferStats.MonthStatus>> getMonthlyTransferStatus(MonthStatusTransfer req);

    Future<List<TransferStats.YearStatus>> getYearlyTransferStatus(YearStatusTransferRequest req);
}