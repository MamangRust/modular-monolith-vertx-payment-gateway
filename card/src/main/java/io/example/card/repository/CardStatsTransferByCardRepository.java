package io.example.card.repository;

import java.util.List;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsTransferByCardRepository {
  Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountBySender(MonthYearCardNumberCard req);

  Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountByReceiver(MonthYearCardNumberCard req);

  Future<List<CardStats.YearAmount>> getYearlyTransferAmountBySender(MonthYearCardNumberCard req);

  Future<List<CardStats.YearAmount>> getYearlyTransferAmountByReceiver(MonthYearCardNumberCard req);
}
