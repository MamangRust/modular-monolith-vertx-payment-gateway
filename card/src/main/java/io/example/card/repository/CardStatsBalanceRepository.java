package io.example.card.repository;

import java.util.List;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsBalanceRepository {
  Future<List<CardStats.MonthBalance>> getMonthlyBalances(int year);
  Future<List<CardStats.YearlyBalance>> getYearlyBalances(int endYear);
}
