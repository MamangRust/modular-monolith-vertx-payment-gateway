package io.example.transaction.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.transaction.domain.requests.YearCardNumberTransactionRequest;
import io.example.transaction.domain.requests.YearTransactionRequest;
import io.example.transaction.model.TransactionStats;
import io.example.transaction.repository.TransactionStatsAmountRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionStatsAmountServiceImplTest {

  @Mock
  private TransactionStatsAmountRepository repository;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private TransactionStatsAmountServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TransactionStatsAmountServiceImpl(repository, redis, metrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
  }

  /* ─── getMonthlyAmounts ─── */

  @Test
  @DisplayName("getMonthlyAmounts cache hit")
  void getMonthlyAmountsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"month":"January","totalAmount":500000}]
        """;
    when(redis.get("transaction:stats:amount:monthly:2026")).thenReturn(Future.succeededFuture(json));

    var req = YearTransactionRequest.builder().year(2026).build();
    service.getMonthlyAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(500_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyAmounts cache miss")
  void getMonthlyAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.MonthAmount("January", 500_000L));

    when(redis.get("transaction:stats:amount:monthly:2026")).thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyAmounts(any(YearTransactionRequest.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTransactionRequest.builder().year(2026).build();
    service.getMonthlyAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(500_000L);
          verify(repository).getMonthlyAmounts(any(YearTransactionRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyAmounts ─── */

  @Test
  @DisplayName("getYearlyAmounts cache hit")
  void getYearlyAmountsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","totalAmount":6000000}]
        """;
    when(redis.get("transaction:stats:amount:yearly:2026")).thenReturn(Future.succeededFuture(json));

    var req = YearTransactionRequest.builder().year(2026).build();
    service.getYearlyAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyAmounts cache miss")
  void getYearlyAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.YearAmount("2026", 6_000_000L));

    when(redis.get("transaction:stats:amount:yearly:2026")).thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyAmounts(any(YearTransactionRequest.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTransactionRequest.builder().year(2026).build();
    service.getYearlyAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          verify(repository).getYearlyAmounts(any(YearTransactionRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyAmountsByCard ─── */

  @Test
  @DisplayName("getMonthlyAmountsByCard cache hit")
  void getMonthlyAmountsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"month":"January","totalAmount":200000}]
        """;
    when(redis.get("transaction:stats:amount:monthly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(json));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111").year(2026).build();
    service.getMonthlyAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(200_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyAmountsByCard cache miss")
  void getMonthlyAmountsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.MonthAmount("January", 200_000L));

    when(redis.get("transaction:stats:amount:monthly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyAmountsByCard(any(YearCardNumberTransactionRequest.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111").year(2026).build();
    service.getMonthlyAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(200_000L);
          verify(repository).getMonthlyAmountsByCard(any(YearCardNumberTransactionRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyAmountsByCard ─── */

  @Test
  @DisplayName("getYearlyAmountsByCard cache hit")
  void getYearlyAmountsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","totalAmount":2400000}]
        """;
    when(redis.get("transaction:stats:amount:yearly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(json));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111").year(2026).build();
    service.getYearlyAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(2_400_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyAmountsByCard cache miss")
  void getYearlyAmountsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.YearAmount("2026", 2_400_000L));

    when(redis.get("transaction:stats:amount:yearly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyAmountsByCard(any(YearCardNumberTransactionRequest.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111").year(2026).build();
    service.getYearlyAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(2_400_000L);
          verify(repository).getYearlyAmountsByCard(any(YearCardNumberTransactionRequest.class));
          ctx.completeNow();
        })));
  }
}
