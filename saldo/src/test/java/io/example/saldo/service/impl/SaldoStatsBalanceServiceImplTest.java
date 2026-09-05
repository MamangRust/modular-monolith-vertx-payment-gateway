package io.example.saldo.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.saldo.model.SaldoStats;
import io.example.saldo.repository.SaldoStatsBalanceRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class SaldoStatsBalanceServiceImplTest {

  @Mock
  private SaldoStatsBalanceRepository repository;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private SaldoStatsBalanceServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new SaldoStatsBalanceServiceImpl(repository, redis, metrics);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(ctx);
  }

  /* ─── getMonthlySaldoBalances ─── */

  @Test
  @DisplayName("getMonthlySaldoBalances returns cached data when available")
  void getMonthlySaldoBalancesCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new SaldoStats.MonthBalance("06", 500_000L));

    when(redis.getJsonList(anyString(), any())).thenAnswer(inv -> Future.succeededFuture(cached));

    service.getMonthlySaldoBalances(2026)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getMonth()).isEqualTo("06");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlySaldoBalances fetches from DB and caches on cache miss")
  void getMonthlySaldoBalancesCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var dbData = List.of(new SaldoStats.MonthBalance("06", 500_000L));

    when(redis.getJsonList(anyString(), any())).thenAnswer(inv -> Future.succeededFuture(null));
    when(repository.getMonthlySaldoBalances(2026)).thenReturn(Future.succeededFuture(dbData));
    when(redis.setJsonList(anyString(), any(), any(Duration.class))).thenAnswer(inv -> Future.succeededFuture());

    service.getMonthlySaldoBalances(2026)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getMonth()).isEqualTo("06");
          verify(repository).getMonthlySaldoBalances(2026);
          verify(redis).setJsonList(anyString(), any(), any(Duration.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlySaldoBalances ─── */

  @Test
  @DisplayName("getYearlySaldoBalances returns cached data when available")
  void getYearlySaldoBalancesCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new SaldoStats.YearBalance("2026", 3_000_000L));

    when(redis.getJsonList(anyString(), any())).thenAnswer(inv -> Future.succeededFuture(cached));

    service.getYearlySaldoBalances(2026)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlySaldoBalances fetches from DB and caches on cache miss")
  void getYearlySaldoBalancesCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var dbData = List.of(new SaldoStats.YearBalance("2026", 3_000_000L));

    when(redis.getJsonList(anyString(), any())).thenAnswer(inv -> Future.succeededFuture(null));
    when(repository.getYearlySaldoBalances(2026)).thenReturn(Future.succeededFuture(dbData));
    when(redis.setJsonList(anyString(), any(), any(Duration.class))).thenAnswer(inv -> Future.succeededFuture());

    service.getYearlySaldoBalances(2026)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          verify(repository).getYearlySaldoBalances(2026);
          ctx.completeNow();
        })));
  }
}
