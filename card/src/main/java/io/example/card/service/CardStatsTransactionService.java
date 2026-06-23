package io.example.card.service;

import java.util.List;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsTransactionService {
  Future<List<CardStats.MonthAmount>> getMonthlyTransactionAmount(int year);

  Future<List<CardStats.YearAmount>> getYearlyTransactionAmount(int endYear);

  Future<List<CardStats.MonthAmount>> getMonthlyTransactionAmountByCardNumber(MonthYearCardNumberCard req);

  Future<List<CardStats.YearAmount>> getYearlyTransactionAmountByCardNumber(MonthYearCardNumberCard req);
}