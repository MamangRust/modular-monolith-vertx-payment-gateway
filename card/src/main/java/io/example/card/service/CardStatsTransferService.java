package io.example.card.service;

import java.util.List;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsTransferService {
  Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountSender(int year);

  Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountReceiver(int year);

  Future<List<CardStats.YearAmount>> getYearlyTransferAmountSender(int endYear);

  Future<List<CardStats.YearAmount>> getYearlyTransferAmountReceiver(int endYear);

  Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountBySender(MonthYearCardNumberCard req);

  Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountByReceiver(MonthYearCardNumberCard req);

  Future<List<CardStats.YearAmount>> getYearlyTransferAmountBySender(MonthYearCardNumberCard req);

  Future<List<CardStats.YearAmount>> getYearlyTransferAmountByReceiver(MonthYearCardNumberCard req);
}