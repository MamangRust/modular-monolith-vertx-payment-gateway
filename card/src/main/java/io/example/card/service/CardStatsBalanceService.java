package io.example.card.service;

import java.util.List;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsBalanceService {
  Future<List<CardStats.MonthBalance>> getMonthlyBalances(int year);

  Future<List<CardStats.YearlyBalance>> getYearlyBalances(int endYear);

  Future<List<CardStats.MonthBalance>> getMonthlyBalancesByCardNumber(MonthYearCardNumberCard req);

  Future<List<CardStats.YearlyBalance>> getYearlyBalancesByCardNumber(MonthYearCardNumberCard req);
}