package io.example.transaction.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.transaction.domain.requests.YearCardNumberTransactionRequest;
import io.example.transaction.domain.requests.YearTransactionRequest;
import io.example.transaction.model.TransactionStats;
import io.example.transaction.repository.TransactionStatsMethodRepository;
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
class TransactionStatsMethodServiceImplTest {

  @Mock
  private TransactionStatsMethodRepository repository;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private TransactionStatsMethodServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TransactionStatsMethodServiceImpl(repository, redis, metrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
  }

  /* ─── getMonthlyMethods ─── */

  @Test
  @DisplayName("getMonthlyMethods cache hit")
  void getMonthlyMethodsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"month":"January","paymentMethod":"BANK","totalTransactions":10,"totalAmount":500000}]
        """;
    when(redis.get("transaction:stats:method:monthly:2026")).thenReturn(Future.succeededFuture(json));

    var req = YearTransactionRequest.builder().year(2026).build();
    service.getMonthlyMethods(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getPaymentMethod()).isEqualTo("BANK");
          assertThat(res.get(0).getTotalTransactions()).isEqualTo(10L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(500_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyMethods cache miss")
  void getMonthlyMethodsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.MonthMethod("January", "BANK", 10L, 500_000L));

    when(redis.get("transaction:stats:method:monthly:2026")).thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyMethods(any(YearTransactionRequest.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTransactionRequest.builder().year(2026).build();
    service.getMonthlyMethods(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getPaymentMethod()).isEqualTo("BANK");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(500_000L);
          verify(repository).getMonthlyMethods(any(YearTransactionRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyMethods ─── */

  @Test
  @DisplayName("getYearlyMethods cache hit")
  void getYearlyMethodsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","paymentMethod":"BANK","totalTransactions":120,"totalAmount":6000000}]
        """;
    when(redis.get("transaction:stats:method:yearly:2026")).thenReturn(Future.succeededFuture(json));

    var req = YearTransactionRequest.builder().year(2026).build();
    service.getYearlyMethods(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getPaymentMethod()).isEqualTo("BANK");
          assertThat(res.get(0).getTotalTransactions()).isEqualTo(120L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyMethods cache miss")
  void getYearlyMethodsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.YearMethod("2026", "BANK", 120L, 6_000_000L));

    when(redis.get("transaction:stats:method:yearly:2026")).thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyMethods(any(YearTransactionRequest.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTransactionRequest.builder().year(2026).build();
    service.getYearlyMethods(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          verify(repository).getYearlyMethods(any(YearTransactionRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyMethodsByCard ─── */

  @Test
  @DisplayName("getMonthlyMethodsByCard cache hit")
  void getMonthlyMethodsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"month":"January","paymentMethod":"BANK","totalTransactions":5,"totalAmount":200000}]
        """;
    when(redis.get("transaction:stats:method:monthly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(json));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111").year(2026).build();
    service.getMonthlyMethodsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getPaymentMethod()).isEqualTo("BANK");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(200_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyMethodsByCard cache miss")
  void getMonthlyMethodsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.MonthMethod("January", "BANK", 5L, 200_000L));

    when(redis.get("transaction:stats:method:monthly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyMethodsByCard(any(YearCardNumberTransactionRequest.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111").year(2026).build();
    service.getMonthlyMethodsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(200_000L);
          verify(repository).getMonthlyMethodsByCard(any(YearCardNumberTransactionRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyMethodsByCard ─── */

  @Test
  @DisplayName("getYearlyMethodsByCard cache hit")
  void getYearlyMethodsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","paymentMethod":"BANK","totalTransactions":60,"totalAmount":2400000}]
        """;
    when(redis.get("transaction:stats:method:yearly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(json));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111").year(2026).build();
    service.getYearlyMethodsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getPaymentMethod()).isEqualTo("BANK");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(2_400_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyMethodsByCard cache miss")
  void getYearlyMethodsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.YearMethod("2026", "BANK", 60L, 2_400_000L));

    when(redis.get("transaction:stats:method:yearly:card:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyMethodsByCard(any(YearCardNumberTransactionRequest.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111").year(2026).build();
    service.getYearlyMethodsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(2_400_000L);
          verify(repository).getYearlyMethodsByCard(any(YearCardNumberTransactionRequest.class));
          ctx.completeNow();
        })));
  }
}
