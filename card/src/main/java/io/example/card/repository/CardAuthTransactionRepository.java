package io.example.card.repository;

import io.example.card.model.CardAuthTransaction;
import io.vertx.core.Future;

import java.util.List;

public interface CardAuthTransactionRepository {
  Future<CardAuthTransaction> insertPending(CardAuthTransaction txn);

  Future<CardAuthTransaction> approve(String txnId, String authCode);

  Future<CardAuthTransaction> decline(String txnId, String declineCode);

  Future<CardAuthTransaction> reverse(String txnId);

  Future<CardAuthTransaction> findByIdempotencyKey(String idempotencyKey);

  Future<CardAuthTransaction> findById(String txnId);

  Future<List<CardAuthTransaction>> findByCardNumber(String cardNumber, int limit, int offset);

  Future<Long> countRecentByCardNumber(String cardNumber, int windowSeconds);

  Future<Integer> updateRiskScore(String txnId, Integer riskScore);
}
