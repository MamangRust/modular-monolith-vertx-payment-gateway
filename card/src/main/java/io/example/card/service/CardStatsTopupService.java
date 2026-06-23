package io.example.card.service;

import java.util.List;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsTopupService {
  Future<List<CardStats.MonthAmount>> getMonthlyTopupAmount(int year);

  Future<List<CardStats.YearAmount>> getYearlyTopupAmount(int endYear);

  Future<List<CardStats.MonthAmount>> getMonthlyTopupAmountByCardNumber(MonthYearCardNumberCard req);

  Future<List<CardStats.YearAmount>> getYearlyTopupAmountByCardNumber(MonthYearCardNumberCard req);
}