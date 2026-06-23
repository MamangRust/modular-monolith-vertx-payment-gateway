package io.example.transfer.repository;

import java.util.List;

import io.example.transfer.domain.requests.MonthStatusTransfer;
import io.example.transfer.domain.requests.YearStatusTransferRequest;
import io.example.transfer.model.TransferStats;
import io.vertx.core.Future;

public interface TransferStatsStatusRepository {
    Future<List<TransferStats.MonthStatus>> getMonthlyTransferStatus(
            MonthStatusTransfer req);

    Future<List<TransferStats.YearStatus>> getYearlyTransferStatus(
            YearStatusTransferRequest req);
}
