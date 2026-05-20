package io.example.card.repository;

import java.util.List;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsTransferByCardRepository {
  Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountBySender(int year, String cardNum);
  Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountByReceiver(int year, String cardNum);
  Future<List<CardStats.YearAmount>> getYearlyTransferAmountBySender(int endYear, String cardNum);
  Future<List<CardStats.YearAmount>> getYearlyTransferAmountByReceiver(int endYear, String cardNum);
}
