package io.example.card.repository;

import java.util.List;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsWithdrawByCardRepository {
  Future<List<CardStats.MonthAmount>> getMonthlyWithdrawAmountByCardNumber(int year, String cardNum);
  Future<List<CardStats.YearAmount>> getYearlyWithdrawAmountByCardNumber(int endYear, String cardNum);
}
