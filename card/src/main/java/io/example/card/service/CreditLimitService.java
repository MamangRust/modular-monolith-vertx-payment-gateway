package io.example.card.service;

import io.example.card.model.CardCreditAccount;
import io.vertx.core.Future;

public interface CreditLimitService {
  Future<CardCreditAccount> getLimit(String cardNumber);

  Future<CardCreditAccount> setLimit(String cardNumber, Long creditLimit,
                                      Integer billingCycleDay, Integer annualRateBps);

  Future<CardCreditAccount> adjustLimit(String cardNumber, Long delta);
}
