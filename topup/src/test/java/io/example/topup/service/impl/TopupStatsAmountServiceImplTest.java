package io.example.topup.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.topup.domain.requests.topup.YearTopupCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupRequest;
import io.example.topup.model.TopupStats;
import io.example.topup.repository.TopupStatsAmountRepository;
import io.example.topup.repository.TopupStatsByCardAmountRepository;
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
class TopupStatsAmountServiceImplTest {

  @Mock
  private TopupStatsAmountRepository repository;

  @Mock
  private TopupStatsByCardAmountRepository cardRepository;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private TopupStatsAmountServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TopupStatsAmountServiceImpl(repository, cardRepository, redis, metrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(metrics.startSpan(anyString(), any())).thenReturn(tc);
  }

  /* ─── getMonthlyTopupAmounts ─── */

  @Test
  @DisplayName("getMonthlyTopupAmounts cache hit")
  void getMonthlyTopupAmountsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"month":"January","totalAmount":500000}]
        """;
    when(redis.get("topup:stats:amount:monthly:2026")).thenReturn(Future.succeededFuture(json));

    var req = YearTopupRequest.builder().year(2026).build();
    service.getMonthlyTopupAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(500_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyTopupAmounts cache miss")
  void getMonthlyTopupAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.MonthAmount("January", 500_000L));

    when(redis.get("topup:stats:amount:monthly:2026")).thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyTopupAmounts(any(YearTopupRequest.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTopupRequest.builder().year(2026).build();
    service.getMonthlyTopupAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(500_000L);
          verify(repository).getMonthlyTopupAmounts(any(YearTopupRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyTopupAmounts ─── */

  @Test
  @DisplayName("getYearlyTopupAmounts cache hit")
  void getYearlyTopupAmountsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","totalAmount":6000000}]
        """;
    when(redis.get("topup:stats:amount:yearly:2026")).thenReturn(Future.succeededFuture(json));

    var req = YearTopupRequest.builder().year(2026).build();
    service.getYearlyTopupAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyTopupAmounts cache miss")
  void getYearlyTopupAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.YearAmount("2026", 6_000_000L));

    when(redis.get("topup:stats:amount:yearly:2026")).thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyTopupAmounts(any(YearTopupRequest.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTopupRequest.builder().year(2026).build();
    service.getYearlyTopupAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          verify(repository).getYearlyTopupAmounts(any(YearTopupRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyTopupAmountsByCard ─── */

  @Test
  @DisplayName("getMonthlyTopupAmountsByCard cache hit")
  void getMonthlyTopupAmountsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"month":"January","totalAmount":200000}]
        """;
    when(redis.get("topup:stats:amount:monthly:card:4111111111111111:2026")).thenReturn(Future.succeededFuture(json));

    var req = YearTopupCardNumberRequest.builder().cardNumber("4111111111111111").year(2026).build();
    service.getMonthlyTopupAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(200_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyTopupAmountsByCard cache miss")
  void getMonthlyTopupAmountsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.MonthAmount("January", 200_000L));

    when(redis.get("topup:stats:amount:monthly:card:4111111111111111:2026")).thenReturn(Future.succeededFuture(null));
    when(cardRepository.getMonthlyTopupAmountsByCard(any(YearTopupCardNumberRequest.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTopupCardNumberRequest.builder().cardNumber("4111111111111111").year(2026).build();
    service.getMonthlyTopupAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(200_000L);
          verify(cardRepository).getMonthlyTopupAmountsByCard(any(YearTopupCardNumberRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyTopupAmountsByCard ─── */

  @Test
  @DisplayName("getYearlyTopupAmountsByCard cache hit")
  void getYearlyTopupAmountsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","totalAmount":2400000}]
        """;
    when(redis.get("topup:stats:amount:yearly:card:4111111111111111:2026")).thenReturn(Future.succeededFuture(json));

    var req = YearTopupCardNumberRequest.builder().cardNumber("4111111111111111").year(2026).build();
    service.getYearlyTopupAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(2_400_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyTopupAmountsByCard cache miss")
  void getYearlyTopupAmountsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.YearAmount("2026", 2_400_000L));

    when(redis.get("topup:stats:amount:yearly:card:4111111111111111:2026")).thenReturn(Future.succeededFuture(null));
    when(cardRepository.getYearlyTopupAmountsByCard(any(YearTopupCardNumberRequest.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTopupCardNumberRequest.builder().cardNumber("4111111111111111").year(2026).build();
    service.getYearlyTopupAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(2_400_000L);
          verify(cardRepository).getYearlyTopupAmountsByCard(any(YearTopupCardNumberRequest.class));
          ctx.completeNow();
        })));
  }
}
