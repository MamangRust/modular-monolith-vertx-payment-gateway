package io.example.card.repository;

import java.util.List;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsTransferRepository {
  Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountSender(int year);
  Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountReceiver(int year);
  Future<List<CardStats.YearAmount>> getYearlyTransferAmountSender(int endYear);
  Future<List<CardStats.YearAmount>> getYearlyTransferAmountReceiver(int endYear);
}
