package io.example.card.service.impl;

import io.example.card.model.CardReward;
import io.example.card.repository.CardRewardRepository;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardRewardServiceImplTest {

  @Mock
  private CardRewardRepository rewardRepo;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private CardRewardServiceImpl service;
  private final UUID txnUuid = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new CardRewardServiceImpl(rewardRepo, redis, metrics);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(ctx);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(ctx);
  }

  private CardReward aReward(Long points) {
    return CardReward.builder()
        .rewardId(1)
        .cardNumber("4111111111111111")
        .txnId(txnUuid)
        .rewardType("POINTS")
        .amount(points)
        .description("Earned from transaction")
        .build();
  }

  @Test
  @DisplayName("earnRewards inserts reward if points > 0 and evicts cache")
  void earnRewardsSuccess(VertxTestContext ctx) {
    mockTracing();
    var reward = aReward(100L); // e.g. amount 10000, 5812 restaurant MCC multiplier is 10. 10000 * 10 / 1000 = 100 points
    when(rewardRepo.addReward(any(CardReward.class))).thenReturn(Future.succeededFuture(reward));
    when(redis.delete("card:4111111111111111:rewards")).thenReturn(Future.succeededFuture(1L));

    service.earnRewards("4111111111111111", txnUuid.toString(), 10000L, "5812")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getAmount()).isEqualTo(100L);
          verify(redis).delete("card:4111111111111111:rewards");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("earnRewards returns null if points calculated is 0 or less")
  void earnRewardsZeroPoints(VertxTestContext ctx) {
    mockTracing();
    service.earnRewards("4111111111111111", txnUuid.toString(), 10L, "5311") // 10 * 5 / 1000 = 0 points
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNull();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getBalance returns cached value on cache hit")
  void getBalanceCacheHit(VertxTestContext ctx) {
    mockTracing();
    when(redis.get("card:4111111111111111:rewards")).thenReturn(Future.succeededFuture("500"));

    service.getBalance("4111111111111111")
        .onComplete(ctx.succeeding(balance -> ctx.verify(() -> {
          assertThat(balance).isEqualTo(500L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getBalance fetches from DB and sets cache on cache miss")
  void getBalanceCacheMiss(VertxTestContext ctx) {
    mockTracing();
    when(redis.get("card:4111111111111111:rewards")).thenReturn(Future.succeededFuture(null));
    when(rewardRepo.getBalance("4111111111111111")).thenReturn(Future.succeededFuture(500L));
    when(redis.set(eq("card:4111111111111111:rewards"), eq("500"), any(Duration.class))).thenReturn(Future.succeededFuture());

    service.getBalance("4111111111111111")
        .onComplete(ctx.succeeding(balance -> ctx.verify(() -> {
          assertThat(balance).isEqualTo(500L);
          verify(redis).set(eq("card:4111111111111111:rewards"), eq("500"), any(Duration.class));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getHistory returns reward history list")
  void getHistorySuccess(VertxTestContext ctx) {
    mockTracing();
    var reward = aReward(100L);
    when(rewardRepo.getHistory("4111111111111111")).thenReturn(Future.succeededFuture(List.of(reward)));

    service.getHistory("4111111111111111")
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("redeemRewards inserts negative reward point entry and evicts cache")
  void redeemRewardsSuccess(VertxTestContext ctx) {
    mockTracing();
    when(rewardRepo.getBalance("4111111111111111")).thenReturn(Future.succeededFuture(1000L));
    when(rewardRepo.redeemRewards("4111111111111111", 500L, "Reward redemption")).thenReturn(Future.succeededFuture(1L));
    when(redis.delete("card:4111111111111111:rewards")).thenReturn(Future.succeededFuture(1L));

    service.redeemRewards("4111111111111111", 500L)
        .onComplete(ctx.succeeding(redeemed -> ctx.verify(() -> {
          assertThat(redeemed).isEqualTo(500L);
          verify(redis).delete("card:4111111111111111:rewards");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("redeemRewards fails with BadRequestException if balance is insufficient")
  void redeemRewardsInsufficientBalance(VertxTestContext ctx) {
    mockTracing();
    when(rewardRepo.getBalance("4111111111111111")).thenReturn(Future.succeededFuture(200L));

    service.redeemRewards("4111111111111111", 500L)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessageContaining("Insufficient rewards balance");
          ctx.completeNow();
        })));
  }
}
