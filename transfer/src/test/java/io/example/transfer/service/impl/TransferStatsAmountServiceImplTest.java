package io.example.transfer.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.transfer.model.TransferStats;
import io.example.transfer.repository.TransferStatsAmountRepository;
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
class TransferStatsAmountServiceImplTest {

  @Mock
  private TransferStatsAmountRepository repository;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private TransferStatsAmountServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TransferStatsAmountServiceImpl(repository, redis, metrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(metrics.startSpan(anyString(), any())).thenReturn(tc);
  }

  /* ─── getMonthlyTransferAmounts ─── */

  @Test
  @DisplayName("getMonthlyTransferAmounts cache hit")
  void getMonthlyTransferAmountsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new TransferStats.MonthAmount("January", 500_000L));

    when(redis.getJsonList("transfer:stats:amount:monthly:2026", TransferStats.MonthAmount.class))
        .thenReturn(Future.succeededFuture(cached));

    service.getMonthlyTransferAmounts(2026)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(500_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyTransferAmounts cache miss")
  void getMonthlyTransferAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransferStats.MonthAmount("January", 500_000L));

    when(redis.getJsonList("transfer:stats:amount:monthly:2026", TransferStats.MonthAmount.class))
        .thenReturn(Future.succeededFuture(List.of()));
    when(repository.getMonthlyTransferAmounts(2026)).thenReturn(Future.succeededFuture(data));
    when(redis.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    service.getMonthlyTransferAmounts(2026)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(500_000L);
          verify(repository).getMonthlyTransferAmounts(2026);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyTransferAmounts ─── */

  @Test
  @DisplayName("getYearlyTransferAmounts cache hit")
  void getYearlyTransferAmountsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new TransferStats.YearAmount("2026", 6_000_000L));

    when(redis.getJsonList("transfer:stats:amount:yearly:2026", TransferStats.YearAmount.class))
        .thenReturn(Future.succeededFuture(cached));

    service.getYearlyTransferAmounts(2026)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyTransferAmounts cache miss")
  void getYearlyTransferAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransferStats.YearAmount("2026", 6_000_000L));

    when(redis.getJsonList("transfer:stats:amount:yearly:2026", TransferStats.YearAmount.class))
        .thenReturn(Future.succeededFuture(List.of()));
    when(repository.getYearlyTransferAmounts(2026)).thenReturn(Future.succeededFuture(data));
    when(redis.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    service.getYearlyTransferAmounts(2026)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          verify(repository).getYearlyTransferAmounts(2026);
          ctx.completeNow();
        })));
  }
}
