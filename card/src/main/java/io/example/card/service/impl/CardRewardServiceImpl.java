package io.example.card.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.card.model.CardReward;
import io.example.card.repository.CardRewardRepository;
import io.example.card.service.CardRewardService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class CardRewardServiceImpl implements CardRewardService {
  private static final Logger log = LoggerFactory.getLogger(CardRewardServiceImpl.class);

  private static final String REDIS_REWARDS_KEY = "card:%s:rewards";
  private static final Duration REDIS_CACHE_TTL = Duration.ofSeconds(300);

  /**
   * MCC-based reward multipliers.
   * Returns points per 1000 unit of currency (paise/satang).
   */
  private static final java.util.Map<String, Long> REWARD_MCC_MAP = java.util.Map.of(
      "5411", 20L,  // Grocery: 2%
      "5812", 10L,  // Restaurant: 1%
      "5813", 10L,  // Bars: 1%
      "5541", 5L,   // Fuel: 0.5%
      "4722", 15L,  // Travel: 1.5%
      "7011", 15L,  // Hotel: 1.5%
      "5311", 5L    // Department store: 0.5%
  );
  private static final long DEFAULT_POINTS_PER_UNIT = 2L; // 0.2% base

  private final CardRewardRepository rewardRepo;
  private final RedisService redis;
  private final TracingMetrics metrics;

  @Override
  public Future<CardReward> earnRewards(String cardNumber, String txnId, Long amount, String mcc) {
    var ctx = metrics.startSpan("CardRewardService.earnRewards");

    long pointsPerUnit = REWARD_MCC_MAP.getOrDefault(mcc != null ? mcc : "", DEFAULT_POINTS_PER_UNIT);
    long pointsEarned = amount * pointsPerUnit / 1000;

    if (pointsEarned <= 0) {
      return Future.succeededFuture(null);
    }

    CardReward reward = CardReward.builder()
        .cardNumber(cardNumber)
        .txnId(txnId != null ? UUID.fromString(txnId) : null)
        .rewardType("POINTS")
        .amount(pointsEarned)
        .description("Earned from " + (mcc != null ? "MCC " + mcc : "transaction"))
        .expiresAt(java.time.LocalDate.now().plusMonths(12)) // 12-month expiry
        .build();

    return rewardRepo.addReward(reward)
        .compose(earned -> redis.delete(String.format(REDIS_REWARDS_KEY, cardNumber)).map(v -> earned))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "earnRewards", pointsEarned + " points"))
        .onFailure(e -> metrics.completeSpanError(ctx, "earnRewards", e.getMessage()));
  }

  @Override
  public Future<Long> getBalance(String cardNumber) {
    var ctx = metrics.startSpan("CardRewardService.getBalance");
    String redisKey = String.format(REDIS_REWARDS_KEY, cardNumber);

    return redis.get(redisKey)
        .compose(cached -> {
          if (cached != null) {
            try {
              return Future.succeededFuture(Long.parseLong(cached));
            } catch (NumberFormatException e) {
              // fall through
            }
          }
          return rewardRepo.getBalance(cardNumber)
              .compose(balance -> redis.set(redisKey, String.valueOf(balance), REDIS_CACHE_TTL)
                  .map(v -> balance));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getBalance", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getBalance", e.getMessage()));
  }

  @Override
  public Future<List<CardReward>> getHistory(String cardNumber) {
    var ctx = metrics.startSpan("CardRewardService.getHistory");
    return rewardRepo.getHistory(cardNumber)
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getHistory", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getHistory", e.getMessage()));
  }

  @Override
  public Future<Long> redeemRewards(String cardNumber, Long points) {
    var ctx = metrics.startSpan("CardRewardService.redeemRewards");

    return rewardRepo.getBalance(cardNumber)
        .compose(balance -> {
          if (balance < points) {
            return Future.failedFuture(
                new io.example.common.exception.grpc.BadRequestException(
                    "Insufficient rewards balance. Available: " + balance + ", requested: " + points));
          }
          return rewardRepo.redeemRewards(cardNumber, points, "Reward redemption")
              .compose(v -> redis.delete(String.format(REDIS_REWARDS_KEY, cardNumber)))
              .map(v -> points);
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "redeemRewards", r + " points redeemed"))
        .onFailure(e -> metrics.completeSpanError(ctx, "redeemRewards", e.getMessage()));
  }
}
