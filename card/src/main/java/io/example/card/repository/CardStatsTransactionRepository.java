package io.example.card.repository;

import java.util.List;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsTransactionRepository {
  Future<List<CardStats.MonthAmount>> getMonthlyTransactionAmount(int year);
  Future<List<CardStats.YearAmount>> getYearlyTransactionAmount(int endYear);
}
