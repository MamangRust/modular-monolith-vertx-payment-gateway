package io.example.card.repository;

import java.util.List;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsBalanceByCardRepository {
  Future<List<CardStats.MonthBalance>> getMonthlyBalancesByCardNumber(int year, String cardNum);
  Future<List<CardStats.YearlyBalance>> getYearlyBalancesByCardNumber(int endYear, String cardNum);
}
