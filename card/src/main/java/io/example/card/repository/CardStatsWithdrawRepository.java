package io.example.card.repository;

import java.util.List;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsWithdrawRepository {
  Future<List<CardStats.MonthAmount>> getMonthlyWithdrawAmount(int year);
  Future<List<CardStats.YearAmount>> getYearlyWithdrawAmount(int endYear);
}
