package io.example.card.repository;

import io.example.card.model.CardReward;
import io.vertx.core.Future;

import java.util.List;

public interface CardRewardRepository {
  Future<CardReward> addReward(CardReward reward);

  Future<Long> getBalance(String cardNumber);

  Future<List<CardReward>> getHistory(String cardNumber);

  Future<Long> redeemRewards(String cardNumber, Long points, String description);

  Future<List<CardReward>> getExpiringRewards(int days);
}
