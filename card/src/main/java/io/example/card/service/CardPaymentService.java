package io.example.card.service;

import io.example.card.model.CardPayment;
import io.vertx.core.Future;

import java.util.List;

public interface CardPaymentService {
  Future<CardPayment> postPayment(String referenceId, String cardNumber, Long amount,
                                   String paymentChannel, Integer statementId);

  Future<List<CardPayment>> getPaymentHistory(String cardNumber, int page, int pageSize);

  Future<Integer> countPayments(String cardNumber);
}
