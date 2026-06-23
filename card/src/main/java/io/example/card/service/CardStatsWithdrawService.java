package io.example.card.service;

import java.util.List;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsWithdrawService {
  Future<List<CardStats.MonthAmount>> getMonthlyWithdrawAmount(int year);

  Future<List<CardStats.YearAmount>> getYearlyWithdrawAmount(int endYear);

  Future<List<CardStats.MonthAmount>> getMonthlyWithdrawAmountByCardNumber(MonthYearCardNumberCard req);

  Future<List<CardStats.YearAmount>> getYearlyWithdrawAmountByCardNumber(MonthYearCardNumberCard req);
}