package io.example.card.repository;

import io.vertx.core.Future;

public interface CardDashboardWithdrawRepository {
  Future<Long> getTotalWithdrawAmount();
  Future<Long> getTotalWithdrawAmountByCardNumber(String cardNumber);
}
