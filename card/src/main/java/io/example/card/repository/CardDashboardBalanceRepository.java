package io.example.card.repository;

import io.vertx.core.Future;

public interface CardDashboardBalanceRepository {
  Future<Long> getTotalBalances();
  Future<Long> getTotalBalanceByCardNumber(String cardNumber);
}
