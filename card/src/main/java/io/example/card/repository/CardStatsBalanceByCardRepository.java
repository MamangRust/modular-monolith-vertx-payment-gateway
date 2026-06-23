package io.example.card.repository;

import java.util.List;
import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsBalanceByCardRepository {
  Future<List<CardStats.MonthBalance>> getMonthlyBalancesByCardNumber(MonthYearCardNumberCard req);

  Future<List<CardStats.YearlyBalance>> getYearlyBalancesByCardNumber(MonthYearCardNumberCard req);
}
