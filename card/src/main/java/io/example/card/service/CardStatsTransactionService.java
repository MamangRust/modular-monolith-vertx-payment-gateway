package io.example.card.service;

import java.util.List;
import io.example.card.model.CardStats;
import io.example.common.domain.ApiResponse;
import io.vertx.core.Future;

public interface CardStatsTransactionService {
  Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransactionAmount(int year);
  Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransactionAmount(int endYear);
  Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransactionAmountByCardNumber(int year, String cardNum);
  Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransactionAmountByCardNumber(int endYear, String cardNum);
}
