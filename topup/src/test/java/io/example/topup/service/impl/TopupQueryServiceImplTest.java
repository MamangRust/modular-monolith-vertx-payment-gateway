package io.example.topup.service.impl;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.topup.domain.requests.topup.FindAllTopups;
import io.example.topup.domain.requests.topup.FindAllTopupsByCardNumber;
import io.example.topup.model.Topup;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.example.topup.repository.TopupQueryRepository;
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

import java.sql.Timestamp;
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
class TopupQueryServiceImplTest {

  @Mock
  private TopupQueryRepository repo;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics tracingMetrics;

  private TopupQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TopupQueryServiceImpl(repo, redisService, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
  }

  private Timestamp now() {
    return Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
  }

  private Topup aTopup() {
    return Topup.builder().id(1).cardNumber("4111111111111111").topupNo("TXN001")
        .topupAmount(500_000L).topupMethod("BANK").topupTime(now())
        .createdAt(now()).updatedAt(now()).build();
  }

  private void stubPagedFallback(PagedResult<Topup> paged) {
    when(repo.getTopups(any(FindAllTopups.class))).thenReturn(Future.succeededFuture(paged));
    when(repo.getTopupsByCardNumber(any(FindAllTopupsByCardNumber.class))).thenReturn(Future.succeededFuture(paged));
    lenient().when(redisService.setJson(anyString(), any(Object.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));
  }

  /* ─── getTopups ─── */

  @Test
  @DisplayName("getTopups returns cached data on cache hit (fallback to DB when deserialization fails)")
  void getTopupsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        {"data":[{"id":1,"cardNumber":"4111111111111111","topupNo":"TXN001","topupAmount":500000,"topupMethod":"BANK","topupTime":"2026-06-26T10:00:00Z","createdAt":"2026-06-26T10:00:00Z","updatedAt":"2026-06-26T10:00:00Z","deletedAt":null}],"totalRecords":1}
        """;
    var paged = new PagedResult<>(List.of(aTopup()), 1);

    when(redisService.get("topup:list::1:10")).thenReturn(Future.succeededFuture(json));
    stubPagedFallback(paged);
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTopups.builder().page(1).pageSize(10).build();
    service.getTopups(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getCardNumber()).isEqualTo("4111111111111111");
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTopups fetches from repo on cache miss")
  void getTopupsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aTopup()), 1);

    when(redisService.get("topup:list::1:10")).thenReturn(Future.succeededFuture(null));
    stubPagedFallback(paged);
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTopups.builder().page(1).pageSize(10).build();
    service.getTopups(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          verify(repo).getTopups(any(FindAllTopups.class));
          ctx.completeNow();
        })));
  }

  /* ─── getTopupsByCardNumber ─── */

  @Test
  @DisplayName("getTopupsByCardNumber returns cached data on cache hit (fallback to DB when deserialization fails)")
  void getTopupsByCardNumberCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        {"data":[{"id":1,"cardNumber":"4111111111111111","topupNo":"TXN001","topupAmount":500000,"topupMethod":"BANK","topupTime":"2026-06-26T10:00:00Z","createdAt":"2026-06-26T10:00:00Z","updatedAt":"2026-06-26T10:00:00Z","deletedAt":null}],"totalRecords":1}
        """;
    var paged = new PagedResult<>(List.of(aTopup()), 1);

    when(redisService.get("topup:card:4111111111111111::1:10")).thenReturn(Future.succeededFuture(json));
    stubPagedFallback(paged);
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTopupsByCardNumber.builder().cardNumber("4111111111111111").page(1).pageSize(10).build();
    service.getTopupsByCardNumber(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTopupsByCardNumber fetches from repo on cache miss")
  void getTopupsByCardNumberCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aTopup()), 1);

    when(redisService.get("topup:card:4111111111111111::1:10")).thenReturn(Future.succeededFuture(null));
    stubPagedFallback(paged);
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTopupsByCardNumber.builder().cardNumber("4111111111111111").page(1).pageSize(10).build();
    service.getTopupsByCardNumber(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          verify(repo).getTopupsByCardNumber(any(FindAllTopupsByCardNumber.class));
          ctx.completeNow();
        })));
  }

  /* ─── getTopupById ─── */

  @Test
  @DisplayName("getTopupById returns topup from cache")
  void getTopupByIdCacheHit(VertxTestContext ctx) {
    mockTracing();
    var topup = aTopup();

    when(redisService.getJson(anyString(), eq(Topup.class))).thenReturn(Future.succeededFuture(topup));

    service.getTopupById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTopupById fetches from repo on cache miss")
  void getTopupByIdCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var topup = aTopup();

    when(redisService.getJson("topup:1", Topup.class)).thenReturn(Future.succeededFuture(null));
    when(repo.getTopupById(1)).thenReturn(Future.succeededFuture(topup));
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getTopupById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(repo).getTopupById(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTopupById fails when topup not found")
  void getTopupByIdNotFound(VertxTestContext ctx) {
    mockTracing();

    when(redisService.getJson("topup:99", Topup.class)).thenReturn(Future.succeededFuture(null));
    when(repo.getTopupById(99)).thenReturn(Future.succeededFuture(null));

    service.getTopupById(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── getTopupByCardNumber ─── */

  @Test
  @DisplayName("getTopupByCardNumber returns topup from cache")
  void getTopupByCardNumberCacheHit(VertxTestContext ctx) {
    mockTracing();
    var topup = aTopup();
    var json = "{\"id\":1,\"cardNumber\":\"4111111111111111\",\"topupNo\":\"TXN001\",\"topupAmount\":500000,\"topupMethod\":\"BANK\",\"topupTime\":\"2026-06-26T10:00:00Z\",\"createdAt\":\"2026-06-26T10:00:00Z\",\"updatedAt\":\"2026-06-26T10:00:00Z\"}";

    when(redisService.getJson(anyString(), eq(Topup.class))).thenReturn(Future.succeededFuture(topup));

    service.getTopupByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTopupByCardNumber fetches from repo on cache miss")
  void getTopupByCardNumberCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var topup = aTopup();

    when(redisService.getJson("topup:card_single:4111111111111111", Topup.class)).thenReturn(Future.succeededFuture(null));
    when(repo.getTopupByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(topup));
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getTopupByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          verify(repo).getTopupByCardNumber("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTopupByCardNumber fails when topup not found")
  void getTopupByCardNumberNotFound(VertxTestContext ctx) {
    mockTracing();

    when(redisService.getJson("topup:card_single:missing", Topup.class)).thenReturn(Future.succeededFuture(null));
    when(repo.getTopupByCardNumber("missing")).thenReturn(Future.succeededFuture(null));

    service.getTopupByCardNumber("missing")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── getActiveTopups (no cache) ─── */

  @Test
  @DisplayName("getActiveTopups delegates to repo, no caching")
  void getActiveTopups(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aTopup()), 1);

    when(repo.getActiveTopups(any(FindAllTopups.class))).thenReturn(Future.succeededFuture(paged));

    var req = FindAllTopups.builder().page(1).pageSize(10).build();
    service.getActiveTopups(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getCardNumber()).isEqualTo("4111111111111111");
          verify(repo).getActiveTopups(any(FindAllTopups.class));
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedTopups (no cache) ─── */

  @Test
  @DisplayName("getTrashedTopups delegates to repo, no caching")
  void getTrashedTopups(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aTopup()), 1);

    when(repo.getTrashedTopups(any(FindAllTopups.class))).thenReturn(Future.succeededFuture(paged));

    var req = FindAllTopups.builder().page(1).pageSize(10).build();
    service.getTrashedTopups(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          verify(repo).getTrashedTopups(any(FindAllTopups.class));
          ctx.completeNow();
        })));
  }
}
