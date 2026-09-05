package io.example.card.service;

import io.example.card.model.CardReward;
import io.vertx.core.Future;

import java.util.List;

public interface CardRewardService {
  Future<CardReward> earnRewards(String cardNumber, String txnId, Long amount, String mcc);

  Future<Long> getBalance(String cardNumber);

  Future<List<CardReward>> getHistory(String cardNumber);

  Future<Long> redeemRewards(String cardNumber, Long points);
}
