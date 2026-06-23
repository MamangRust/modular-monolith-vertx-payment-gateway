package io.example.card.repository;

import java.util.List;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsTopupByCardRepository {
  Future<List<CardStats.MonthAmount>> getMonthlyTopupAmountByCardNumber(MonthYearCardNumberCard req);

  Future<List<CardStats.YearAmount>> getYearlyTopupAmountByCardNumber(MonthYearCardNumberCard req);
}
