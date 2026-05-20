package io.example.card.repository;

import io.vertx.core.Future;

public interface CardDashboardTransactionRepository {
  Future<Long> getTotalTransactionAmount();
  Future<Long> getTotalTransactionAmountByCardNumber(String cardNumber);
}
