package io.example.card.repository;

import io.example.card.model.CardPayment;
import io.vertx.core.Future;

import java.util.List;

public interface CardPaymentRepository {
  Future<CardPayment> findByReferenceId(String referenceId);

  Future<CardPayment> insertPayment(CardPayment payment);

  Future<List<CardPayment>> findByCardNumber(String cardNumber, int limit, int offset);

  Future<Integer> countByCardNumber(String cardNumber);

  Future<Long> totalPaymentsByCardNumber(String cardNumber);

  Future<CardPayment> findById(String paymentId);
}
