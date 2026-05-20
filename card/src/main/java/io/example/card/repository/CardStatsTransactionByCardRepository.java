package io.example.card.repository;

import java.util.List;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsTransactionByCardRepository {
  Future<List<CardStats.MonthAmount>> getMonthlyTransactionAmountByCardNumber(int year, String cardNum);
  Future<List<CardStats.YearAmount>> getYearlyTransactionAmountByCardNumber(int endYear, String cardNum);
}
