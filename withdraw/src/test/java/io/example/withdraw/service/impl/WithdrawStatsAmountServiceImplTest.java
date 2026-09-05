package io.example.withdraw.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.withdraw.domain.requests.YearMonthCardNumber;
import io.example.withdraw.model.WithdrawStats;
import io.example.withdraw.repository.WithdrawStatsAmountRepository;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class WithdrawStatsAmountServiceImplTest {

  @Mock
  private WithdrawStatsAmountRepository repository;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics metrics;

  private WithdrawStatsAmountServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new WithdrawStatsAmountServiceImpl(repository, redisService, metrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
  }

  private void stubSetJson() {
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));
  }

  private static final int YEAR = 2026;

  /* ─── getMonthlyWithdrawAmounts ─── */

  @Test
  @DisplayName("getMonthlyWithdrawAmounts returns cached data on cache hit")
  void getMonthlyWithdrawAmountsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cachedJson = new JsonArray()
        .add(new JsonObject().put("month", "2026-01").put("totalAmount", 1_000_000L))
        .add(new JsonObject().put("month", "2026-02").put("totalAmount", 2_000_000L))
        .encode();

    when(redisService.get(eq("withdraw:stats:amount:monthly:" + YEAR)))
        .thenReturn(Future.succeededFuture(cachedJson));

    service.getMonthlyWithdrawAmounts(YEAR)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(2);
          assertThat(result.get(0).getMonth()).isEqualTo("2026-01");
          assertThat(result.get(0).getTotalAmount()).isEqualTo(1_000_000L);
          verify(redisService).get(eq("withdraw:stats:amount:monthly:" + YEAR));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyWithdrawAmounts fetches from repo on cache miss, caches result")
  void getMonthlyWithdrawAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(
        new WithdrawStats.MonthAmount("2026-01", 1_000_000L),
        new WithdrawStats.MonthAmount("2026-02", 2_000_000L));

    when(redisService.get(eq("withdraw:stats:amount:monthly:" + YEAR)))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyWithdrawAmounts(YEAR)).thenReturn(Future.succeededFuture(data));
    stubSetJson();

    service.getMonthlyWithdrawAmounts(YEAR)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(2);
          assertThat(result.get(0).getMonth()).isEqualTo("2026-01");
          verify(repository).getMonthlyWithdrawAmounts(YEAR);
          verify(redisService).setJson(
              eq("withdraw:stats:amount:monthly:" + YEAR),
              any(Object.class), any(Duration.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyWithdrawAmounts ─── */

  @Test
  @DisplayName("getYearlyWithdrawAmounts returns cached data on cache hit")
  void getYearlyWithdrawAmountsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cachedJson = new JsonArray()
        .add(new JsonObject().put("year", "2026").put("totalAmount", 5_000_000L))
        .encode();

    when(redisService.get(eq("withdraw:stats:amount:yearly:" + YEAR)))
        .thenReturn(Future.succeededFuture(cachedJson));

    service.getYearlyWithdrawAmounts(YEAR)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          assertThat(result.get(0).getTotalAmount()).isEqualTo(5_000_000L);
          verify(redisService).get(eq("withdraw:stats:amount:yearly:" + YEAR));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyWithdrawAmounts fetches from repo on cache miss, caches result")
  void getYearlyWithdrawAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new WithdrawStats.YearAmount("2026", 5_000_000L));

    when(redisService.get(eq("withdraw:stats:amount:yearly:" + YEAR)))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyWithdrawAmounts(YEAR)).thenReturn(Future.succeededFuture(data));
    stubSetJson();

    service.getYearlyWithdrawAmounts(YEAR)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          verify(repository).getYearlyWithdrawAmounts(YEAR);
          verify(redisService).setJson(
              eq("withdraw:stats:amount:yearly:" + YEAR),
              any(Object.class), any(Duration.class));
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyWithdrawAmountsByCard ─── */

  @Test
  @DisplayName("getMonthlyWithdrawAmountsByCard returns cached data on cache hit")
  void getMonthlyWithdrawAmountsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var req = new YearMonthCardNumber("4111111111111111", YEAR);
    var cachedJson = new JsonArray()
        .add(new JsonObject().put("month", "2026-03").put("totalAmount", 750_000L))
        .encode();

    when(redisService.get(eq("withdraw:stats:amount:card:monthly:4111111111111111:" + YEAR)))
        .thenReturn(Future.succeededFuture(cachedJson));

    service.getMonthlyWithdrawAmountsByCard(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getMonth()).isEqualTo("2026-03");
          assertThat(result.get(0).getTotalAmount()).isEqualTo(750_000L);
          verify(redisService).get(
              eq("withdraw:stats:amount:card:monthly:4111111111111111:" + YEAR));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyWithdrawAmountsByCard fetches from repo on cache miss, caches result")
  void getMonthlyWithdrawAmountsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var req = new YearMonthCardNumber("4111111111111111", YEAR);
    var data = List.of(new WithdrawStats.MonthAmount("2026-03", 750_000L));

    when(redisService.get(eq("withdraw:stats:amount:card:monthly:4111111111111111:" + YEAR)))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyWithdrawAmountsByCard(eq(req)))
        .thenReturn(Future.succeededFuture(data));
    stubSetJson();

    service.getMonthlyWithdrawAmountsByCard(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getMonth()).isEqualTo("2026-03");
          verify(repository).getMonthlyWithdrawAmountsByCard(eq(req));
          verify(redisService).setJson(
              eq("withdraw:stats:amount:card:monthly:4111111111111111:" + YEAR),
              any(Object.class), any(Duration.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyWithdrawAmountsByCard ─── */

  @Test
  @DisplayName("getYearlyWithdrawAmountsByCard returns cached data on cache hit")
  void getYearlyWithdrawAmountsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var req = new YearMonthCardNumber("4111111111111111", YEAR);
    var cachedJson = new JsonArray()
        .add(new JsonObject().put("year", "2026").put("totalAmount", 3_000_000L))
        .encode();

    when(redisService.get(eq("withdraw:stats:amount:card:yearly:4111111111111111:" + YEAR)))
        .thenReturn(Future.succeededFuture(cachedJson));

    service.getYearlyWithdrawAmountsByCard(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          assertThat(result.get(0).getTotalAmount()).isEqualTo(3_000_000L);
          verify(redisService).get(
              eq("withdraw:stats:amount:card:yearly:4111111111111111:" + YEAR));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyWithdrawAmountsByCard fetches from repo on cache miss, caches result")
  void getYearlyWithdrawAmountsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var req = new YearMonthCardNumber("4111111111111111", YEAR);
    var data = List.of(new WithdrawStats.YearAmount("2026", 3_000_000L));

    when(redisService.get(eq("withdraw:stats:amount:card:yearly:4111111111111111:" + YEAR)))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyWithdrawAmountsByCard(eq(req)))
        .thenReturn(Future.succeededFuture(data));
    stubSetJson();

    service.getYearlyWithdrawAmountsByCard(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          verify(repository).getYearlyWithdrawAmountsByCard(eq(req));
          verify(redisService).setJson(
              eq("withdraw:stats:amount:card:yearly:4111111111111111:" + YEAR),
              any(Object.class), any(Duration.class));
          ctx.completeNow();
        })));
  }
}
