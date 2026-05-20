package io.example.card.repository;

import io.vertx.core.Future;

public interface CardDashboardTopupRepository {
  Future<Long> getTotalTopAmount();
  Future<Long> getTotalTopupAmountByCardNumber(String cardNumber);
}
