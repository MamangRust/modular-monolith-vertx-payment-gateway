package io.example.withdraw.service.impl;

import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.withdraw.domain.requests.FindAllWithdrawCardNumber;
import io.example.withdraw.domain.requests.FindAllWithdraws;
import io.example.withdraw.model.Withdraw;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.example.withdraw.repository.WithdrawQueryRepository;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class WithdrawQueryServiceImplTest {

  @Mock
  private WithdrawQueryRepository repo;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics tracingMetrics;

  private WithdrawQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new WithdrawQueryServiceImpl(repo, redisService, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
  }

  private OffsetDateTime now() {
    return OffsetDateTime.of(2026, 6, 26, 10, 0, 0, 0, ZoneOffset.UTC);
  }

  private Withdraw aWithdraw() {
    return Withdraw.builder()
        .id(1)
        .withdrawNo("WDR001")
        .cardNumber("4111111111111111")
        .withdrawAmount(500_000L)
        .status("success")
        .withdrawTime(now())
        .createdAt(now())
        .updatedAt(now())
        .build();
  }

  /* ─── getWithdraws ─── */

  @Test
  @DisplayName("getWithdraws returns paginated withdrawals (cache miss)")
  void getWithdrawsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var req = new FindAllWithdraws(1, 10, "");
    var withdraw = aWithdraw();
    var paged = new PagedResult<>(List.of(withdraw), 1);

    when(redisService.get(eq("withdraw:list::1:10"))).thenReturn(Future.succeededFuture(null));
    when(repo.getWithdraws(eq(req))).thenReturn(Future.succeededFuture(paged));
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    service.getWithdraws(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          assertThat(result.getData().get(0).getId()).isEqualTo(1);
          verify(repo).getWithdraws(eq(req));
          verify(redisService).setJson(eq("withdraw:list::1:10"), eq(paged), any(Duration.class));
          ctx.completeNow();
        })));
  }

  /* ─── getWithdrawsByCardNumber ─── */

  @Test
  @DisplayName("getWithdrawsByCardNumber returns paginated withdrawals by card (cache miss)")
  void getWithdrawsByCardNumberCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var req = new FindAllWithdrawCardNumber("4111111111111111", 1, 10, "");
    var withdraw = aWithdraw();
    var paged = new PagedResult<>(List.of(withdraw), 1);

    when(redisService.get(eq("withdraw:card:4111111111111111::1:10")))
        .thenReturn(Future.succeededFuture(null));
    when(repo.getWithdrawsByCardNumber(eq("4111111111111111"), eq(""), eq(1), eq(10)))
        .thenReturn(Future.succeededFuture(paged));
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    service.getWithdrawsByCardNumber(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          verify(repo).getWithdrawsByCardNumber(eq("4111111111111111"), eq(""), eq(1), eq(10));
          verify(redisService).setJson(
              eq("withdraw:card:4111111111111111::1:10"), eq(paged), any(Duration.class));
          ctx.completeNow();
        })));
  }

  /* ─── getActiveWithdraws ─── */

  @Test
  @DisplayName("getActiveWithdraws returns active withdrawals (no cache)")
  void getActiveWithdrawsSuccess(VertxTestContext ctx) {
    mockTracing();
    var req = new FindAllWithdraws(1, 10, "");
    var withdraw = aWithdraw();
    var paged = new PagedResult<>(List.of(withdraw), 1);

    when(repo.getActiveWithdraws(eq(req))).thenReturn(Future.succeededFuture(paged));

    service.getActiveWithdraws(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          assertThat(result.getData().get(0).getId()).isEqualTo(1);
          verify(repo).getActiveWithdraws(eq(req));
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedWithdraws ─── */

  @Test
  @DisplayName("getTrashedWithdraws returns trashed withdrawals (no cache)")
  void getTrashedWithdrawsSuccess(VertxTestContext ctx) {
    mockTracing();
    var req = new FindAllWithdraws(1, 10, "");
    var trashed = aWithdraw();
    trashed.setDeletedAt(now());
    var paged = new PagedResult<>(List.of(trashed), 1);

    when(repo.getTrashedWithdraws(eq(req))).thenReturn(Future.succeededFuture(paged));

    service.getTrashedWithdraws(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          verify(repo).getTrashedWithdraws(eq(req));
          ctx.completeNow();
        })));
  }

  /* ─── getWithdrawById ─── */

  @Test
  @DisplayName("getWithdrawById returns withdrawal from cache hit")
  void getWithdrawByIdCacheHit(VertxTestContext ctx) {
    mockTracing();
    var withdraw = aWithdraw();

    when(redisService.getJson(eq("withdraw:1"), eq(Withdraw.class)))
        .thenReturn(Future.succeededFuture(withdraw));

    service.getWithdrawById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          verify(redisService).getJson(eq("withdraw:1"), eq(Withdraw.class));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getWithdrawById fetches from repo on cache miss, caches result")
  void getWithdrawByIdCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var withdraw = aWithdraw();

    when(redisService.getJson(eq("withdraw:1"), eq(Withdraw.class)))
        .thenReturn(Future.succeededFuture(null));
    when(repo.getWithdrawById(1)).thenReturn(Future.succeededFuture(withdraw));
    when(redisService.setJson(eq("withdraw:1"), any(Object.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    service.getWithdrawById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(repo).getWithdrawById(1);
          verify(redisService).setJson(eq("withdraw:1"), eq(withdraw), any(Duration.class));
          ctx.completeNow();
        })));
  }

  /* ─── getWithdrawsByCardNumberPrimitive ─── */

  @Test
  @DisplayName("getWithdrawsByCardNumberPrimitive returns withdrawals from cache hit")
  void getWithdrawsByCardNumberPrimitiveCacheHit(VertxTestContext ctx) {
    mockTracing();
    var withdraw = aWithdraw();

    when(redisService.getJsonList(eq("withdraw:card_primitive:4111111111111111"), eq(Withdraw.class)))
        .thenReturn(Future.succeededFuture(List.of(withdraw)));

    service.getWithdrawsByCardNumberPrimitive("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getId()).isEqualTo(1);
          verify(redisService).getJsonList(
              eq("withdraw:card_primitive:4111111111111111"), eq(Withdraw.class));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getWithdrawsByCardNumberPrimitive fetches from repo on cache miss, caches result")
  void getWithdrawsByCardNumberPrimitiveCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var withdraw = aWithdraw();

    when(redisService.getJsonList(eq("withdraw:card_primitive:4111111111111111"), eq(Withdraw.class)))
        .thenReturn(Future.succeededFuture(List.of()));
    when(repo.getWithdrawsByCardNumberPrimitive(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(List.of(withdraw)));
    when(redisService.setJsonList(anyString(), anyList(), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    service.getWithdrawsByCardNumberPrimitive("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getId()).isEqualTo(1);
          verify(repo).getWithdrawsByCardNumberPrimitive(eq("4111111111111111"));
          verify(redisService).setJsonList(
              eq("withdraw:card_primitive:4111111111111111"),
              eq(List.of(withdraw)), any(Duration.class));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getWithdrawsByCardNumberPrimitive returns empty list when repo returns null")
  void getWithdrawsByCardNumberPrimitiveRepoNull(VertxTestContext ctx) {
    mockTracing();

    when(redisService.getJsonList(eq("withdraw:card_primitive:4111111111111111"), eq(Withdraw.class)))
        .thenReturn(Future.succeededFuture(List.of()));
    when(repo.getWithdrawsByCardNumberPrimitive(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(null));

    service.getWithdrawsByCardNumberPrimitive("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isEmpty();
          verify(repo).getWithdrawsByCardNumberPrimitive(eq("4111111111111111"));
          ctx.completeNow();
        })));
  }
}
