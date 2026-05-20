package io.example.card.repository;

import java.util.List;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsTopupByCardRepository {
  Future<List<CardStats.MonthAmount>> getMonthlyTopupAmountByCardNumber(int year, String cardNum);
  Future<List<CardStats.YearAmount>> getYearlyTopupAmountByCardNumber(int endYear, String cardNum);
}
