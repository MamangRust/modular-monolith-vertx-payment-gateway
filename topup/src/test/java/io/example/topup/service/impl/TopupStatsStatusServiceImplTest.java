package io.example.topup.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.topup.domain.requests.topup.MonthTopupStatusCardNumberRequest;
import io.example.topup.domain.requests.topup.MonthTopupStatusRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusRequest;
import io.example.topup.model.TopupStats;
import io.example.topup.repository.TopupStatsByCardStatusRepository;
import io.example.topup.repository.TopupStatsStatusRepository;
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
class TopupStatsStatusServiceImplTest {

  @Mock
  private TopupStatsStatusRepository repository;

  @Mock
  private TopupStatsByCardStatusRepository cardRepository;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private TopupStatsStatusServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TopupStatsStatusServiceImpl(repository, cardRepository, redis, metrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(metrics.startSpan(anyString(), any())).thenReturn(tc);
  }

  /* ─── getMonthlyTopupStatus ─── */

  @Test
  @DisplayName("getMonthlyTopupStatus cache hit")
  void getMonthlyTopupStatusCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","month":"June","totalCount":15,"totalAmount":750000}]
        """;
    when(redis.get("topup:stats:status:monthly:success:2026:6")).thenReturn(Future.succeededFuture(json));

    var req = MonthTopupStatusRequest.builder().year(2026).month(6).status("success").build();
    service.getMonthlyTopupStatus(req)
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
  @DisplayName("getMonthlyTopupStatus cache miss")
  void getMonthlyTopupStatusCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.MonthStatus("2026", "June", 15L, 750_000L));

    when(redis.get("topup:stats:status:monthly:success:2026:6")).thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyTopupStatus(any(MonthTopupStatusRequest.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = MonthTopupStatusRequest.builder().year(2026).month(6).status("success").build();
    service.getMonthlyTopupStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalCount()).isEqualTo(15L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(750_000L);
          verify(repository).getMonthlyTopupStatus(any(MonthTopupStatusRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyTopupStatus ─── */

  @Test
  @DisplayName("getYearlyTopupStatus cache hit")
  void getYearlyTopupStatusCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","totalCount":180,"totalAmount":9000000}]
        """;
    when(redis.get("topup:stats:status:yearly:success:2026")).thenReturn(Future.succeededFuture(json));

    var req = YearTopupStatusRequest.builder().year(2026).status("success").build();
    service.getYearlyTopupStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalCount()).isEqualTo(180L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(9_000_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyTopupStatus cache miss")
  void getYearlyTopupStatusCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.YearStatus("2026", 180L, 9_000_000L));

    when(redis.get("topup:stats:status:yearly:success:2026")).thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyTopupStatus(any(YearTopupStatusRequest.class))).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTopupStatusRequest.builder().year(2026).status("success").build();
    service.getYearlyTopupStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalCount()).isEqualTo(180L);
          verify(repository).getYearlyTopupStatus(any(YearTopupStatusRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyTopupStatusByCard ─── */

  @Test
  @DisplayName("getMonthlyTopupStatusByCard cache hit")
  void getMonthlyTopupStatusByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","month":"June","totalCount":8,"totalAmount":400000}]
        """;
    when(redis.get("topup:stats:status:monthly:card:success:4111111111111111:2026:6"))
        .thenReturn(Future.succeededFuture(json));

    var req = MonthTopupStatusCardNumberRequest.builder()
        .cardNumber("4111111111111111").year(2026).month(6).status("success").build();
    service.getMonthlyTopupStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getMonth()).isEqualTo("June");
          assertThat(res.get(0).getTotalCount()).isEqualTo(8L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyTopupStatusByCard cache miss")
  void getMonthlyTopupStatusByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.MonthStatus("2026", "June", 8L, 400_000L));

    when(redis.get("topup:stats:status:monthly:card:success:4111111111111111:2026:6"))
        .thenReturn(Future.succeededFuture(null));
    when(cardRepository.getMonthlyTopupStatusByCard(any(MonthTopupStatusCardNumberRequest.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = MonthTopupStatusCardNumberRequest.builder()
        .cardNumber("4111111111111111").year(2026).month(6).status("success").build();
    service.getMonthlyTopupStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(400_000L);
          verify(cardRepository).getMonthlyTopupStatusByCard(any(MonthTopupStatusCardNumberRequest.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyTopupStatusByCard ─── */

  @Test
  @DisplayName("getYearlyTopupStatusByCard cache hit")
  void getYearlyTopupStatusByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"year":"2026","totalCount":96,"totalAmount":4800000}]
        """;
    when(redis.get("topup:stats:status:yearly:card:success:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(json));

    var req = YearTopupStatusCardNumberRequest.builder()
        .cardNumber("4111111111111111").year(2026).status("success").build();
    service.getYearlyTopupStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalCount()).isEqualTo(96L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(4_800_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyTopupStatusByCard cache miss")
  void getYearlyTopupStatusByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TopupStats.YearStatus("2026", 96L, 4_800_000L));

    when(redis.get("topup:stats:status:yearly:card:success:4111111111111111:2026"))
        .thenReturn(Future.succeededFuture(null));
    when(cardRepository.getYearlyTopupStatusByCard(any(YearTopupStatusCardNumberRequest.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = YearTopupStatusCardNumberRequest.builder()
        .cardNumber("4111111111111111").year(2026).status("success").build();
    service.getYearlyTopupStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(4_800_000L);
          verify(cardRepository).getYearlyTopupStatusByCard(any(YearTopupStatusCardNumberRequest.class));
          ctx.completeNow();
        })));
  }
}
