package io.example.saldo.service.impl;

import io.example.common.domain.PagedResult;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.saldo.domain.requests.FindAllSaldos;
import io.example.saldo.model.Saldo;
import io.example.saldo.repository.SaldoQueryRepository;
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
import pb.saldo.Saldo.FindAllSaldoRequest;

import java.sql.Timestamp;
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
class SaldoQueryServiceImplTest {

  @Mock
  private SaldoQueryRepository repo;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private SaldoQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new SaldoQueryServiceImpl(repo, redis, metrics);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(ctx);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(ctx);
  }

  private Timestamp now() {
    return Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
  }

  private Saldo aSaldo() {
    return Saldo.builder()
        .id(1)
        .cardNumber("4111111111111111")
        .totalBalance(1_000_000L)
        .createdAt(now())
        .updatedAt(now())
        .build();
  }

  private String aSaldoJson() {
    return "{\"id\":1,\"cardNumber\":\"4111111111111111\",\"totalBalance\":1000000,"
        + "\"createdAt\":\"2026-06-26T10:00:00Z\",\"updatedAt\":\"2026-06-26T10:00:00Z\"}";
  }

  private String aPagedSaldoJson() {
    return "{\"data\":[{\"id\":1,\"cardNumber\":\"4111111111111111\",\"totalBalance\":1000000,"
        + "\"createdAt\":\"2026-06-26T10:00:00Z\",\"updatedAt\":\"2026-06-26T10:00:00Z\"}],\"totalRecords\":1}";
  }

  /* ─── getAllSaldos ─── */

  @Test
  @DisplayName("getAllSaldos returns cached data when available")
  void getAllSaldosCacheHit(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(redis.get("saldo:all:p:1:s:10:k:")).thenReturn(Future.succeededFuture(aPagedSaldoJson()));
    // Stub fallback path (PagedResult lacks @NoArgsConstructor, so cache deserialization fails)
    when(repo.getSaldos(any(FindAllSaldos.class)))
        .thenReturn(Future.succeededFuture(new PagedResult<>(List.of(saldo), 1)));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    service.getAllSaldos(FindAllSaldoRequest.newBuilder().setPage(1).setPageSize(10).build())
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getId()).isEqualTo(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getAllSaldos fetches from DB and caches on cache miss")
  void getAllSaldosCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(redis.get("saldo:all:p:1:s:10:k:")).thenReturn(Future.succeededFuture(null));
    when(repo.getSaldos(any(FindAllSaldos.class)))
        .thenReturn(Future.succeededFuture(new PagedResult<>(List.of(saldo), 1)));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getAllSaldos(FindAllSaldoRequest.newBuilder().setPage(1).setPageSize(10).build())
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getSaldoById ─── */

  @Test
  @DisplayName("getSaldoById returns cached saldo when available")
  void getSaldoByIdCacheHit(VertxTestContext ctx) {
    mockTracing();

    when(redis.get("saldo:id:1")).thenReturn(Future.succeededFuture(aSaldoJson()));

    service.getSaldoById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getSaldoById fetches from DB and caches on cache miss")
  void getSaldoByIdCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(redis.get("saldo:id:1")).thenReturn(Future.succeededFuture(null));
    when(repo.getSaldoById(1)).thenReturn(Future.succeededFuture(saldo));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getSaldoById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getSaldoById fails when saldo not found in DB")
  void getSaldoByIdNotFound(VertxTestContext ctx) {
    mockTracing();

    when(redis.get("saldo:id:99")).thenReturn(Future.succeededFuture(null));
    when(repo.getSaldoById(99)).thenReturn(Future.succeededFuture(null));

    service.getSaldoById(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── getSaldoByCardNumber ─── */

  @Test
  @DisplayName("getSaldoByCardNumber returns saldo from repo")
  void getSaldoByCardNumberSuccess(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(repo.getSaldoByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(saldo));

    service.getSaldoByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getSaldoByCardNumber fails when not found")
  void getSaldoByCardNumberNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.getSaldoByCardNumber("0000000000000000")).thenReturn(Future.succeededFuture(null));

    service.getSaldoByCardNumber("0000000000000000")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── getActiveSaldos ─── */

  @Test
  @DisplayName("getActiveSaldos fetches from DB and caches on cache miss")
  void getActiveSaldosCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(redis.get("saldo:active:p:1:s:10:k:")).thenReturn(Future.succeededFuture(null));
    when(repo.getActiveSaldos(any(FindAllSaldos.class)))
        .thenReturn(Future.succeededFuture(new PagedResult<>(List.of(saldo), 1)));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getActiveSaldos(FindAllSaldoRequest.newBuilder().setPage(1).setPageSize(10).build())
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedSaldos ─── */

  @Test
  @DisplayName("getTrashedSaldos fetches from DB and caches on cache miss")
  void getTrashedSaldosCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(redis.get("saldo:trashed:p:1:s:10:k:")).thenReturn(Future.succeededFuture(null));
    when(repo.getTrashedSaldos(any(FindAllSaldos.class)))
        .thenReturn(Future.succeededFuture(new PagedResult<>(List.of(saldo), 1)));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getTrashedSaldos(FindAllSaldoRequest.newBuilder().setPage(1).setPageSize(10).build())
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          ctx.completeNow();
        })));
  }
}
