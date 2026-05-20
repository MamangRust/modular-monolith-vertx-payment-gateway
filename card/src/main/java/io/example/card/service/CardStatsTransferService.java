package io.example.card.service;

import java.util.List;
import io.example.card.model.CardStats;
import io.example.common.domain.ApiResponse;
import io.vertx.core.Future;

public interface CardStatsTransferService {
  Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransferAmountSender(int year);
  Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransferAmountReceiver(int year);
  Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransferAmountSender(int endYear);
  Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransferAmountReceiver(int endYear);
  Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransferAmountBySender(int year, String cardNum);
  Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransferAmountByReceiver(int year, String cardNum);
  Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransferAmountBySender(int endYear, String cardNum);
  Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransferAmountByReceiver(int endYear, String cardNum);
}
