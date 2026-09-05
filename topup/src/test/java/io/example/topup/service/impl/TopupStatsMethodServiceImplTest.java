package io.example.topup.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.topup.domain.requests.topup.YearTopupCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupRequest;
import io.example.topup.model.TopupStats;
import io.example.topup.repository.TopupStatsByCardMethodRepository;
import io.example.topup.repository.TopupStatsMethodRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TopupStatsMethodServiceImplTest {

  @Mock
  private TopupStatsMethodRepository repository;

  @Mock
  private TopupStatsByCardMethodRepository cardRepository;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private TopupStatsMethodServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TopupStatsMethodServiceImpl(repository, cardRepository, redis, metrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(metrics.startSpan(anyString(), any())).thenReturn(tc);
  }

  /* ─── getMonthlyTopupMethods ─── */

  @Test
  @DisplayName("getMonthlyTopupMethods cache hit")
  void getMonthlyTopupMethodsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"month":"January","topupMethod":"BANK","totalTopups":10,"totalAmount":500000}]
        """;
    when(redis.get("topup:stats:method:monthly:2026")).thenReturn(Future.succeededFuture(json));

    var req = YearTopupRequest.builder().year(2026).build();
    service.getMonthlyTopupMethods(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getTopupMethod()).isEqualTo("BANK");
          assertThat(res.get(0).getTotalTopups()).isEqualTo(10L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(500_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyTopupMethods cache miss")
  void getMonthlyTopupMethodsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.MonthMethod("January", "BANK", 10L, 500_000L));

    when(redis.get("topup:stats:method:monthly:2026")).thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyTopupMethods(any(YearTopupRequest.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTopupRequest.builder().year(2026).build();
    service.getMonthlyTopupMethods(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTopupMethod()).isEqualTo("BANK");
          assertThat(res.get(0).getTotalTopups()).isEqualTo(10L);
          verify(repository).getMonthlyTopupMethods(any(YearTopupRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyTopupMethods ─── */

  @Test
  @DisplayName("getYearlyTopupMethods cache hit")
  void getYearlyTopupMethodsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","topupMethod":"BANK","totalTopups":120,"totalAmount":6000000}]
        """;
    when(redis.get("topup:stats:method:yearly:2026")).thenReturn(Future.succeededFuture(json));

    var req = YearTopupRequest.builder().year(2026).build();
    service.getYearlyTopupMethods(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTopupMethod()).isEqualTo("BANK");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyTopupMethods cache miss")
  void getYearlyTopupMethodsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.YearMethod("2026", "BANK", 120L, 6_000_000L));

    when(redis.get("topup:stats:method:yearly:2026")).thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyTopupMethods(any(YearTopupRequest.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTopupRequest.builder().year(2026).build();
    service.getYearlyTopupMethods(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          verify(repository).getYearlyTopupMethods(any(YearTopupRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyTopupMethodsByCard ─── */

  @Test
  @DisplayName("getMonthlyTopupMethodsByCard cache hit")
  void getMonthlyTopupMethodsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"month":"January","topupMethod":"BANK","totalTopups":5,"totalAmount":200000}]
        """;
    when(redis.get("topup:stats:method:monthly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(json));

    var req = YearTopupCardNumberRequest.builder().cardNumber("4111111111111111").year(2026).build();
    service.getMonthlyTopupMethodsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getTopupMethod()).isEqualTo("BANK");
          assertThat(res.get(0).getTotalTopups()).isEqualTo(5L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyTopupMethodsByCard cache miss")
  void getMonthlyTopupMethodsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.MonthMethod("January", "BANK", 5L, 200_000L));

    when(redis.get("topup:stats:method:monthly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(null));
    when(cardRepository.getMonthlyTopupMethodsByCard(any(YearTopupCardNumberRequest.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTopupCardNumberRequest.builder().cardNumber("4111111111111111").year(2026).build();
    service.getMonthlyTopupMethodsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(200_000L);
          verify(cardRepository).getMonthlyTopupMethodsByCard(any(YearTopupCardNumberRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyTopupMethodsByCard ─── */

  @Test
  @DisplayName("getYearlyTopupMethodsByCard cache hit")
  void getYearlyTopupMethodsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","topupMethod":"BANK","totalTopups":60,"totalAmount":2400000}]
        """;
    when(redis.get("topup:stats:method:yearly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(json));

    var req = YearTopupCardNumberRequest.builder().cardNumber("4111111111111111").year(2026).build();
    service.getYearlyTopupMethodsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTopupMethod()).isEqualTo("BANK");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(2_400_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyTopupMethodsByCard cache miss")
  void getYearlyTopupMethodsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.YearMethod("2026", "BANK", 60L, 2_400_000L));

    when(redis.get("topup:stats:method:yearly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(null));
    when(cardRepository.getYearlyTopupMethodsByCard(any(YearTopupCardNumberRequest.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTopupCardNumberRequest.builder().cardNumber("4111111111111111").year(2026).build();
    service.getYearlyTopupMethodsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(2_400_000L);
          verify(cardRepository).getYearlyTopupMethodsByCard(any(YearTopupCardNumberRequest.class));
          ctx.completeNow();
        })));
  }
}
