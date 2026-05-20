package io.example.card.service;

import java.util.List;
import io.example.card.model.CardStats;
import io.example.common.domain.ApiResponse;
import io.vertx.core.Future;

public interface CardStatsWithdrawService {
  Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyWithdrawAmount(int year);
  Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyWithdrawAmount(int endYear);
  Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyWithdrawAmountByCardNumber(int year, String cardNum);
  Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyWithdrawAmountByCardNumber(int endYear, String cardNum);
}
