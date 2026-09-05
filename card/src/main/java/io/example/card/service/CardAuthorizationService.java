package io.example.card.service;

import io.example.card.model.CardAuthTransaction;
import io.vertx.core.Future;

public interface CardAuthorizationService {
  Future<CardAuthTransaction> authorize(String cardNumber, Integer merchantId, Long amount,
                                         String currency, String posEntryMode, String mcc,
                                         String idempotencyKey);

  Future<CardAuthTransaction> reverse(String txnId, String cardNumber, Long amount, String idempotencyKey);
}
