package io.example.card.service;

import java.util.List;
import io.example.card.model.CardStats;
import io.example.common.domain.ApiResponse;
import io.vertx.core.Future;

public interface CardStatsBalanceService {
  Future<ApiResponse<List<CardStats.MonthBalance>>> getMonthlyBalances(int year);
  Future<ApiResponse<List<CardStats.YearlyBalance>>> getYearlyBalances(int endYear);
  Future<ApiResponse<List<CardStats.MonthBalance>>> getMonthlyBalancesByCardNumber(int year, String cardNum);
  Future<ApiResponse<List<CardStats.YearlyBalance>>> getYearlyBalancesByCardNumber(int endYear, String cardNum);
}
