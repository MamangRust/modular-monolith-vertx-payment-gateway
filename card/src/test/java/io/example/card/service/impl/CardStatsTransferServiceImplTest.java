package io.example.card.service.impl;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsTransferByCardRepository;
import io.example.card.repository.CardStatsTransferRepository;
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
class CardStatsTransferServiceImplTest {
  @Mock private CardStatsTransferRepository repo; @Mock private CardStatsTransferByCardRepository byCard;
  @Mock private RedisService redis; @Mock private TracingMetrics metrics;
  private CardStatsTransferServiceImpl service;
  @BeforeEach void setUp() { service = new CardStatsTransferServiceImpl(repo, byCard, redis, metrics); }
  void mockTracing() { var c = new TracingContext(Context.root(), Instant.now()); lenient().when(metrics.startSpan(anyString())).thenReturn(c); lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(c); }

  @Test void monthlySender(VertxTestContext ctx) { mockTracing(); when(redis.get("stats:transfer:monthly:sender:2026")).thenReturn(Future.succeededFuture(null)); when(repo.getMonthlyTransferAmountSender(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(1, 5000L)))); when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK")); service.getMonthlyTransferAmountSender(2026).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
  @Test void monthlyReceiver(VertxTestContext ctx) { mockTracing(); when(redis.get("stats:transfer:monthly:receiver:2026")).thenReturn(Future.succeededFuture(null)); when(repo.getMonthlyTransferAmountReceiver(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(1, 3000L)))); when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK")); service.getMonthlyTransferAmountReceiver(2026).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
  @Test void byCardSender(VertxTestContext ctx) { mockTracing(); var r = MonthYearCardNumberCard.builder().year(2026).cardNumber("4111").build(); when(redis.get("stats:transfer:monthly:sender:2026:4111")).thenReturn(Future.succeededFuture(null)); when(byCard.getMonthlyTransferAmountBySender(r)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(3, 4000L)))); when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK")); service.getMonthlyTransferAmountBySender(r).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
  @Test void byCardReceiver(VertxTestContext ctx) { mockTracing(); var r = MonthYearCardNumberCard.builder().year(2026).cardNumber("4111").build(); when(redis.get("stats:transfer:monthly:receiver:2026:4111")).thenReturn(Future.succeededFuture(null)); when(byCard.getMonthlyTransferAmountByReceiver(r)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(3, 2000L)))); when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK")); service.getMonthlyTransferAmountByReceiver(r).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); }))); }
}
