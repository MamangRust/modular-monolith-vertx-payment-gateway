package io.example.card.repository;

import io.example.card.model.CardCreditAccount;
import io.vertx.core.Future;

import java.util.List;

public interface CardCreditAccountRepository {
  Future<CardCreditAccount> findByCardNumber(String cardNumber);

  Future<CardCreditAccount> createAccount(String cardNumber, Long creditLimit, Integer billingCycleDay, Integer annualRateBps);

  Future<CardCreditAccount> decrementAvailableCredit(String cardNumber, Long amount);

  Future<CardCreditAccount> releaseCredit(String cardNumber, Long amount);

  Future<CardCreditAccount> updateStatus(String cardNumber, String status);

  Future<List<CardCreditAccount>> findAccountsDueForBilling(Integer cycleDay);

  Future<CardCreditAccount> setCreditLimit(String cardNumber, Long creditLimit);

  Future<CardCreditAccount> adjustCreditLimit(String cardNumber, Long delta);

  Future<Boolean> deleteByCardNumber(String cardNumber);
}
