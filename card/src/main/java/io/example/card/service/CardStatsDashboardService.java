package io.example.card.service;

import io.example.card.model.CardStats;
import io.example.common.domain.ApiResponse;
import io.vertx.core.Future;

public interface CardStatsDashboardService {
  Future<ApiResponse<CardStats.Dashboard>> getDashboardCard();
  Future<ApiResponse<CardStats.DashboardByCardNumber>> getDashboardCardByCardNumber(String cardNumber);
}
