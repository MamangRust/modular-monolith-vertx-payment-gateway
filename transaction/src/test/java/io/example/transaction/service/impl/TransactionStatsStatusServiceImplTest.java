package io.example.transaction.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.transaction.domain.requests.MonthStatusTransaction;
import io.example.transaction.domain.requests.MonthStatusTransactionCardNumber;
import io.example.transaction.domain.requests.YearStatusTransaction;
import io.example.transaction.domain.requests.YearStatusTransactionCardNumber;
import io.example.transaction.model.TransactionStats;
import io.example.transaction.repository.TransactionStatsStatusRepository;
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
class TransactionStatsStatusServiceImplTest {

  @Mock
  private TransactionStatsStatusRepository repository;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private TransactionStatsStatusServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TransactionStatsStatusServiceImpl(repository, redis, metrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
  }

  /* ─── getMonthlyStatus ─── */

  @Test
  @DisplayName("getMonthlyStatus cache hit")
  void getMonthlyStatusCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","month":"June","totalCount":15,"totalAmount":750000}]
        """;
    when(redis.get("transaction:stats:status:monthly:success:2026:6"))
        .thenReturn(Future.succeededFuture(json));

    var req = MonthStatusTransaction.builder().year(2026).month(6).status("success").build();
    service.getMonthlyStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getMonth()).isEqualTo("June");
          assertThat(res.get(0).getTotalCount()).isEqualTo(15L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(750_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyStatus cache miss")
  void getMonthlyStatusCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.MonthStatus("2026", "June", 15L, 750_000L));

    when(redis.get("transaction:stats:status:monthly:success:2026:6"))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyStatus(any(MonthStatusTransaction.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = MonthStatusTransaction.builder().year(2026).month(6).status("success").build();
    service.getMonthlyStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalCount()).isEqualTo(15L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(750_000L);
          verify(repository).getMonthlyStatus(any(MonthStatusTransaction.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyStatus ─── */

  @Test
  @DisplayName("getYearlyStatus cache hit")
  void getYearlyStatusCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","totalCount":180,"totalAmount":9000000}]
        """;
    when(redis.get("transaction:stats:status:yearly:success:2026"))
        .thenReturn(Future.succeededFuture(json));

    var req = YearStatusTransaction.builder().year(2026).status("success").build();
    service.getYearlyStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalCount()).isEqualTo(180L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(9_000_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyStatus cache miss")
  void getYearlyStatusCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.YearStatus("2026", 180L, 9_000_000L));

    when(redis.get("transaction:stats:status:yearly:success:2026"))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyStatus(any(YearStatusTransaction.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearStatusTransaction.builder().year(2026).status("success").build();
    service.getYearlyStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalCount()).isEqualTo(180L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(9_000_000L);
          verify(repository).getYearlyStatus(any(YearStatusTransaction.class));
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyStatusByCard ─── */

  @Test
  @DisplayName("getMonthlyStatusByCard cache hit")
  void getMonthlyStatusByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","month":"June","totalCount":8,"totalAmount":400000}]
        """;
    when(redis.get("transaction:stats:status:monthly:card:success:4111111111111111:2026:6"))
        .thenReturn(Future.succeededFuture(json));

    var req = MonthStatusTransactionCardNumber.builder()
        .cardNumber("4111111111111111").year(2026).month(6).status("success").build();
    service.getMonthlyStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getMonth()).isEqualTo("June");
          assertThat(res.get(0).getTotalCount()).isEqualTo(8L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(400_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyStatusByCard cache miss")
  void getMonthlyStatusByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.MonthStatus("2026", "June", 8L, 400_000L));

    when(redis.get("transaction:stats:status:monthly:card:success:4111111111111111:2026:6"))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyStatusByCard(any(MonthStatusTransactionCardNumber.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = MonthStatusTransactionCardNumber.builder()
        .cardNumber("4111111111111111").year(2026).month(6).status("success").build();
    service.getMonthlyStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalCount()).isEqualTo(8L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(400_000L);
          verify(repository).getMonthlyStatusByCard(any(MonthStatusTransactionCardNumber.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyStatusByCard ─── */

  @Test
  @DisplayName("getYearlyStatusByCard cache hit")
  void getYearlyStatusByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","totalCount":96,"totalAmount":4800000}]
        """;
    when(redis.get("transaction:stats:status:yearly:card:success:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(json));

    var req = YearStatusTransactionCardNumber.builder()
        .cardNumber("4111111111111111").year(2026).status("success").build();
    service.getYearlyStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalCount()).isEqualTo(96L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(4_800_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyStatusByCard cache miss")
  void getYearlyStatusByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransactionStats.YearStatus("2026", 96L, 4_800_000L));

    when(redis.get("transaction:stats:status:yearly:card:success:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyStatusByCard(any(YearStatusTransactionCardNumber.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearStatusTransactionCardNumber.builder()
        .cardNumber("4111111111111111").year(2026).status("success").build();
    service.getYearlyStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalCount()).isEqualTo(96L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(4_800_000L);
          verify(repository).getYearlyStatusByCard(any(YearStatusTransactionCardNumber.class));
          ctx.completeNow();
        })));
  }
}
