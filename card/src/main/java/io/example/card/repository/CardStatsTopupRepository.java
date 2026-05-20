package io.example.card.repository;

import java.util.List;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsTopupRepository {
  Future<List<CardStats.MonthAmount>> getMonthlyTopupAmount(int year);
  Future<List<CardStats.YearAmount>> getYearlyTopupAmount(int endYear);
}
