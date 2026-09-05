package io.example.withdraw.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.withdraw.domain.requests.MonthStatusWithdrawCardNumber;
import io.example.withdraw.domain.requests.YearStatusWithdrawCardNumber;
import io.example.withdraw.model.WithdrawStats;
import io.example.withdraw.repository.WithdrawStatsStatusRepository;
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
class WithdrawStatsStatusServiceImplTest {

  @Mock
  private WithdrawStatsStatusRepository repository;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics metrics;

  private WithdrawStatsStatusServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new WithdrawStatsStatusServiceImpl(repository, redisService, metrics);
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
  private static final int MONTH = 6;
  private static final String STATUS = "success";
  private static final String CARD_NUMBER = "4111111111111111";

  /* ─── getMonthlyWithdrawStatus ─── */

  @Test
  @DisplayName("getMonthlyWithdrawStatus returns cached data on cache hit")
  void getMonthlyWithdrawStatusCacheHit(VertxTestContext ctx) {
    mockTracing();
    var req = new MonthStatusWithdrawCardNumber(CARD_NUMBER, YEAR, MONTH, STATUS);
    var cachedJson = new JsonArray()
        .add(new JsonObject()
            .put("year", "2026").put("month", "06")
            .put("totalCount", 10L).put("totalAmount", 5_000_000L))
        .encode();

    when(redisService.get(eq("withdraw:stats:status:monthly:" + YEAR + ":" + MONTH + ":" + STATUS)))
        .thenReturn(Future.succeededFuture(cachedJson));

    service.getMonthlyWithdrawStatus(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          assertThat(result.get(0).getMonth()).isEqualTo("06");
          assertThat(result.get(0).getTotalCount()).isEqualTo(10L);
          assertThat(result.get(0).getTotalAmount()).isEqualTo(5_000_000L);
          verify(redisService).get(
              eq("withdraw:stats:status:monthly:" + YEAR + ":" + MONTH + ":" + STATUS));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyWithdrawStatus fetches from repo on cache miss, caches result")
  void getMonthlyWithdrawStatusCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var req = new MonthStatusWithdrawCardNumber(CARD_NUMBER, YEAR, MONTH, STATUS);
    var data = List.of(new WithdrawStats.MonthStatus("2026", "06", 10L, 5_000_000L));

    when(redisService.get(eq("withdraw:stats:status:monthly:" + YEAR + ":" + MONTH + ":" + STATUS)))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyWithdrawStatus(eq(req))).thenReturn(Future.succeededFuture(data));
    stubSetJson();

    service.getMonthlyWithdrawStatus(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          assertThat(result.get(0).getMonth()).isEqualTo("06");
          assertThat(result.get(0).getTotalCount()).isEqualTo(10L);
          verify(repository).getMonthlyWithdrawStatus(eq(req));
          verify(redisService).setJson(
              eq("withdraw:stats:status:monthly:" + YEAR + ":" + MONTH + ":" + STATUS),
              any(Object.class), any(Duration.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyWithdrawStatus ─── */

  @Test
  @DisplayName("getYearlyWithdrawStatus returns cached data on cache hit")
  void getYearlyWithdrawStatusCacheHit(VertxTestContext ctx) {
    mockTracing();
    var req = new YearStatusWithdrawCardNumber(CARD_NUMBER, YEAR, STATUS);
    var cachedJson = new JsonArray()
        .add(new JsonObject()
            .put("year", "2026")
            .put("totalCount", 25L).put("totalAmount", 12_000_000L))
        .encode();

    when(redisService.get(eq("withdraw:stats:status:yearly:" + YEAR + ":" + STATUS)))
        .thenReturn(Future.succeededFuture(cachedJson));

    service.getYearlyWithdrawStatus(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          assertThat(result.get(0).getTotalCount()).isEqualTo(25L);
          assertThat(result.get(0).getTotalAmount()).isEqualTo(12_000_000L);
          verify(redisService).get(
              eq("withdraw:stats:status:yearly:" + YEAR + ":" + STATUS));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyWithdrawStatus fetches from repo on cache miss, caches result")
  void getYearlyWithdrawStatusCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var req = new YearStatusWithdrawCardNumber(CARD_NUMBER, YEAR, STATUS);
    var data = List.of(new WithdrawStats.YearStatus("2026", 25L, 12_000_000L));

    when(redisService.get(eq("withdraw:stats:status:yearly:" + YEAR + ":" + STATUS)))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyWithdrawStatus(eq(req))).thenReturn(Future.succeededFuture(data));
    stubSetJson();

    service.getYearlyWithdrawStatus(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          assertThat(result.get(0).getTotalCount()).isEqualTo(25L);
          verify(repository).getYearlyWithdrawStatus(eq(req));
          verify(redisService).setJson(
              eq("withdraw:stats:status:yearly:" + YEAR + ":" + STATUS),
              any(Object.class), any(Duration.class));
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyStatusByCard ─── */

  @Test
  @DisplayName("getMonthlyStatusByCard returns cached data on cache hit")
  void getMonthlyStatusByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var req = new MonthStatusWithdrawCardNumber(CARD_NUMBER, YEAR, MONTH, STATUS);
    var cachedJson = new JsonArray()
        .add(new JsonObject()
            .put("year", "2026").put("month", "06")
            .put("totalCount", 5L).put("totalAmount", 2_500_000L))
        .encode();

    when(redisService.get(eq(
        "withdraw:stats:status:card:monthly:" + CARD_NUMBER + ":" + YEAR + ":" + MONTH + ":" + STATUS)))
        .thenReturn(Future.succeededFuture(cachedJson));

    service.getMonthlyStatusByCard(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          assertThat(result.get(0).getMonth()).isEqualTo("06");
          assertThat(result.get(0).getTotalCount()).isEqualTo(5L);
          verify(redisService).get(eq(
              "withdraw:stats:status:card:monthly:" + CARD_NUMBER + ":" + YEAR + ":" + MONTH + ":" + STATUS));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyStatusByCard fetches from repo on cache miss, caches result")
  void getMonthlyStatusByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var req = new MonthStatusWithdrawCardNumber(CARD_NUMBER, YEAR, MONTH, STATUS);
    var data = List.of(new WithdrawStats.MonthStatus("2026", "06", 5L, 2_500_000L));

    when(redisService.get(eq(
        "withdraw:stats:status:card:monthly:" + CARD_NUMBER + ":" + YEAR + ":" + MONTH + ":" + STATUS)))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getMonthlyStatusByCard(eq(req))).thenReturn(Future.succeededFuture(data));
    stubSetJson();

    service.getMonthlyStatusByCard(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          assertThat(result.get(0).getMonth()).isEqualTo("06");
          assertThat(result.get(0).getTotalCount()).isEqualTo(5L);
          verify(repository).getMonthlyStatusByCard(eq(req));
          verify(redisService).setJson(eq(
              "withdraw:stats:status:card:monthly:" + CARD_NUMBER + ":" + YEAR + ":" + MONTH + ":" + STATUS),
              any(Object.class), any(Duration.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyStatusByCard ─── */

  @Test
  @DisplayName("getYearlyStatusByCard returns cached data on cache hit")
  void getYearlyStatusByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var req = new YearStatusWithdrawCardNumber(CARD_NUMBER, YEAR, STATUS);
    var cachedJson = new JsonArray()
        .add(new JsonObject()
            .put("year", "2026")
            .put("totalCount", 15L).put("totalAmount", 8_000_000L))
        .encode();

    when(redisService.get(eq(
        "withdraw:stats:status:card:yearly:" + CARD_NUMBER + ":" + YEAR + ":" + STATUS)))
        .thenReturn(Future.succeededFuture(cachedJson));

    service.getYearlyStatusByCard(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          assertThat(result.get(0).getTotalCount()).isEqualTo(15L);
          assertThat(result.get(0).getTotalAmount()).isEqualTo(8_000_000L);
          verify(redisService).get(eq(
              "withdraw:stats:status:card:yearly:" + CARD_NUMBER + ":" + YEAR + ":" + STATUS));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyStatusByCard fetches from repo on cache miss, caches result")
  void getYearlyStatusByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var req = new YearStatusWithdrawCardNumber(CARD_NUMBER, YEAR, STATUS);
    var data = List.of(new WithdrawStats.YearStatus("2026", 15L, 8_000_000L));

    when(redisService.get(eq(
        "withdraw:stats:status:card:yearly:" + CARD_NUMBER + ":" + YEAR + ":" + STATUS)))
        .thenReturn(Future.succeededFuture(null));
    when(repository.getYearlyStatusByCard(eq(req))).thenReturn(Future.succeededFuture(data));
    stubSetJson();

    service.getYearlyStatusByCard(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getYear()).isEqualTo("2026");
          assertThat(result.get(0).getTotalCount()).isEqualTo(15L);
          verify(repository).getYearlyStatusByCard(eq(req));
          verify(redisService).setJson(eq(
              "withdraw:stats:status:card:yearly:" + CARD_NUMBER + ":" + YEAR + ":" + STATUS),
              any(Object.class), any(Duration.class));
          ctx.completeNow();
        })));
  }
}
