package io.example.card.service.impl;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsWithdrawByCardRepository;
import io.example.card.repository.CardStatsWithdrawRepository;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardStatsWithdrawServiceImplTest {
  @Mock private CardStatsWithdrawRepository repo; @Mock private CardStatsWithdrawByCardRepository byCard;
  @Mock private RedisService redis; @Mock private TracingMetrics metrics;
  private CardStatsWithdrawServiceImpl service;
  @BeforeEach void setUp() { service = new CardStatsWithdrawServiceImpl(repo, byCard, redis, metrics); }
  void mockTracing() { var c = new TracingContext(Context.root(), Instant.now()); lenient().when(metrics.startSpan(anyString())).thenReturn(c); lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(c); }

  @Test void monthlyCacheHit(VertxTestContext ctx) { mockTracing(); when(redis.get("stats:withdraw:monthly:2026")).thenReturn(Future.succeededFuture("[{\"month\":1,\"amount\":2000}]")); service.getMonthlyWithdrawAmount(2026).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
  @Test void monthlyCacheMiss(VertxTestContext ctx) { mockTracing(); when(redis.get("stats:withdraw:monthly:2026")).thenReturn(Future.succeededFuture(null)); when(repo.getMonthlyWithdrawAmount(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(1, 2000L)))); when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK")); service.getMonthlyWithdrawAmount(2026).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
  @Test void yearlyCacheMiss(VertxTestContext ctx) { mockTracing(); when(redis.get("stats:withdraw:yearly:2026")).thenReturn(Future.succeededFuture(null)); when(repo.getYearlyWithdrawAmount(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.YearAmount(2025, 24000L)))); when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK")); service.getYearlyWithdrawAmount(2026).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
  @Test void byCardCacheMiss(VertxTestContext ctx) { mockTracing(); var r = MonthYearCardNumberCard.builder().year(2026).cardNumber("4111").build(); when(redis.get("stats:withdraw:monthly:2026:4111")).thenReturn(Future.succeededFuture(null)); when(byCard.getMonthlyWithdrawAmountByCardNumber(r)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(3, 1500L)))); when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK")); service.getMonthlyWithdrawAmountByCardNumber(r).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
}
