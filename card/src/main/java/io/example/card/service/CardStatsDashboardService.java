package io.example.card.service;

import io.example.card.model.CardStats;
import io.vertx.core.Future;

public interface CardStatsDashboardService {
  Future<CardStats.Dashboard> getDashboardCard();

  Future<CardStats.DashboardByCardNumber> getDashboardCardByCardNumber(String cardNumber);
}