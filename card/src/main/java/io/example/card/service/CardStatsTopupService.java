package io.example.card.service;

import java.util.List;
import io.example.card.model.CardStats;
import io.example.common.domain.ApiResponse;
import io.vertx.core.Future;

public interface CardStatsTopupService {
  Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTopupAmount(int year);
  Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTopupAmount(int endYear);
  Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTopupAmountByCardNumber(int year, String cardNum);
  Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTopupAmountByCardNumber(int endYear, String cardNum);
}
