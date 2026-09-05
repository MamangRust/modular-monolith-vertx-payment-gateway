package io.example.card.service.impl;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsBalanceByCardRepository;
import io.example.card.repository.CardStatsBalanceRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardStatsBalanceServiceImplTest {

  @Mock
  private CardStatsBalanceRepository repository;

  @Mock
  private CardStatsBalanceByCardRepository byCardRepository;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private CardStatsBalanceServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new CardStatsBalanceServiceImpl(repository, byCardRepository, redis, metrics);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(ctx);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(ctx);
  }

  /* ─── getMonthlyBalances ─── */

  @Test
  @DisplayName("getMonthlyBalances returns from cache when available")
  void getMonthlyBalancesCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = "[{\"month\":1,\"balance\":1000},{\"month\":2,\"balance\":2000}]";

    when(redis.get("stats:balance:monthly:2026")).thenReturn(Future.succeededFuture(json));

    service.getMonthlyBalances(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(2);
          assertThat(list.get(0).getMonth()).isEqualTo(1);
          assertThat(list.get(0).getBalance()).isEqualTo(1000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyBalances fetches from repo and caches on miss")
  void getMonthlyBalancesCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var dbList = List.of(
        new CardStats.MonthBalance(1, 1000L),
        new CardStats.MonthBalance(2, 2000L));

    when(redis.get("stats:balance:monthly:2026")).thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyBalances(2026)).thenReturn(Future.succeededFuture(dbList));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getMonthlyBalances(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(2);
          verify(repository).getMonthlyBalances(2026);
          verify(redis).setJson(eq("stats:balance:monthly:2026"), eq(dbList), eq(Duration.ofMinutes(15)));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyBalances ─── */

  @Test
  @DisplayName("getYearlyBalances returns from cache when available")
  void getYearlyBalancesCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = "[{\"year\":2025,\"balance\":50000}]";

    when(redis.get("stats:balance:yearly:2026")).thenReturn(Future.succeededFuture(json));

    service.getYearlyBalances(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo(2025);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyBalances fetches from repo and caches on miss")
  void getYearlyBalancesCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var dbList = List.of(new CardStats.YearlyBalance(2025, 50000L));

    when(redis.get("stats:balance:yearly:2026")).thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyBalances(2026)).thenReturn(Future.succeededFuture(dbList));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getYearlyBalances(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          verify(repository).getYearlyBalances(2026);
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyBalancesByCardNumber ─── */

  @Test
  @DisplayName("getMonthlyBalancesByCardNumber returns from cache")
  void getMonthlyBalancesByCardNumberCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = "[{\"month\":3,\"balance\":3000}]";
    var req = MonthYearCardNumberCard.builder().year(2026).cardNumber("4111").build();

    when(redis.get("stats:balance:monthly:2026:4111")).thenReturn(Future.succeededFuture(json));

    service.getMonthlyBalancesByCardNumber(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo(3);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyBalancesByCardNumber fetches from byCardRepo on miss")
  void getMonthlyBalancesByCardNumberCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var req = MonthYearCardNumberCard.builder().year(2026).cardNumber("4111").build();
    var dbList = List.of(new CardStats.MonthBalance(3, 3000L));

    when(redis.get("stats:balance:monthly:2026:4111")).thenReturn(Future.succeededFuture(null));
    when(byCardRepository.getMonthlyBalancesByCardNumber(req)).thenReturn(Future.succeededFuture(dbList));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getMonthlyBalancesByCardNumber(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          verify(byCardRepository).getMonthlyBalancesByCardNumber(req);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyBalancesByCardNumber ─── */

  @Test
  @DisplayName("getYearlyBalancesByCardNumber fetches from byCardRepo on miss")
  void getYearlyBalancesByCardNumberCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var req = MonthYearCardNumberCard.builder().year(2026).cardNumber("4111").build();
    var dbList = List.of(new CardStats.YearlyBalance(2025, 40000L));

    when(redis.get("stats:balance:yearly:2026:4111")).thenReturn(Future.succeededFuture(null));
    when(byCardRepository.getYearlyBalancesByCardNumber(req)).thenReturn(Future.succeededFuture(dbList));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getYearlyBalancesByCardNumber(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          verify(byCardRepository).getYearlyBalancesByCardNumber(req);
          ctx.completeNow();
        })));
  }
}
