package io.example.card.service.impl;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsTopupByCardRepository;
import io.example.card.repository.CardStatsTopupRepository;
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
class CardStatsTopupServiceImplTest {
  @Mock private CardStatsTopupRepository repo; @Mock private CardStatsTopupByCardRepository byCard;
  @Mock private RedisService redis; @Mock private TracingMetrics metrics;
  private CardStatsTopupServiceImpl service;
  @BeforeEach void setUp() { service = new CardStatsTopupServiceImpl(repo, byCard, redis, metrics); }
  void mockTracing() { var c = new TracingContext(Context.root(), Instant.now()); lenient().when(metrics.startSpan(anyString())).thenReturn(c); lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(c); }

  @Test void monthlyCacheHit(VertxTestContext ctx) { mockTracing(); when(redis.get("stats:topup:monthly:2026")).thenReturn(Future.succeededFuture("[{\"month\":1,\"amount\":5000}]")); service.getMonthlyTopupAmount(2026).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
  @Test void monthlyCacheMiss(VertxTestContext ctx) { mockTracing(); when(redis.get("stats:topup:monthly:2026")).thenReturn(Future.succeededFuture(null)); when(repo.getMonthlyTopupAmount(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(1, 5000L)))); when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK")); service.getMonthlyTopupAmount(2026).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
  @Test void yearlyCacheMiss(VertxTestContext ctx) { mockTracing(); when(redis.get("stats:topup:yearly:2026")).thenReturn(Future.succeededFuture(null)); when(repo.getYearlyTopupAmount(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.YearAmount(2025, 60000L)))); when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK")); service.getYearlyTopupAmount(2026).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
  @Test void byCardCacheMiss(VertxTestContext ctx) { mockTracing(); var r = MonthYearCardNumberCard.builder().year(2026).cardNumber("4111").build(); when(redis.get("stats:topup:monthly:2026:4111")).thenReturn(Future.succeededFuture(null)); when(byCard.getMonthlyTopupAmountByCardNumber(r)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(3, 3000L)))); when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK")); service.getMonthlyTopupAmountByCardNumber(r).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
}
