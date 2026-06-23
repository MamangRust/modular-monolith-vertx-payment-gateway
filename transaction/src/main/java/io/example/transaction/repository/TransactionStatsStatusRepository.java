package io.example.transaction.repository;

import java.util.List;
import io.example.transaction.model.TransactionStats;
import io.vertx.core.Future;

import io.example.transaction.domain.requests.MonthStatusTransaction;
import io.example.transaction.domain.requests.MonthStatusTransactionCardNumber;
import io.example.transaction.domain.requests.YearStatusTransaction;
import io.example.transaction.domain.requests.YearStatusTransactionCardNumber;

public interface TransactionStatsStatusRepository {
    Future<List<TransactionStats.MonthStatus>> getMonthlyStatus(MonthStatusTransaction req);

    Future<List<TransactionStats.YearStatus>> getYearlyStatus(YearStatusTransaction req);

    Future<List<TransactionStats.MonthStatus>> getMonthlyStatusByCard(MonthStatusTransactionCardNumber req);

    Future<List<TransactionStats.YearStatus>> getYearlyStatusByCard(YearStatusTransactionCardNumber req);
}
