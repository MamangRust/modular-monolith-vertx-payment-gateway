package io.example.transfer.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.transfer.domain.requests.MonthStatusTransfer;
import io.example.transfer.domain.requests.YearStatusTransferRequest;
import io.example.transfer.model.TransferStats;
import io.example.transfer.repository.TransferStatsStatusRepository;
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
class TransferStatsStatusServiceImplTest {

  @Mock
  private TransferStatsStatusRepository repository;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private TransferStatsStatusServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TransferStatsStatusServiceImpl(repository, redis, metrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(metrics.startSpan(anyString(), any())).thenReturn(tc);
  }

  /* ─── getMonthlyTransferStatus ─── */

  @Test
  @DisplayName("getMonthlyTransferStatus cache hit")
  void getMonthlyTransferStatusCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new TransferStats.MonthStatus("2026", "January", 10L, 1_000_000L));

    when(redis.getJsonList("transfer:stats:status:monthly:2026:1:success",
        TransferStats.MonthStatus.class)).thenReturn(Future.succeededFuture(cached));

    var req = new MonthStatusTransfer(2026, 1, "success");
    service.getMonthlyTransferStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getTotalCount()).isEqualTo(10L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(1_000_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyTransferStatus cache miss")
  void getMonthlyTransferStatusCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransferStats.MonthStatus("2026", "January", 10L, 1_000_000L));

    when(redis.getJsonList("transfer:stats:status:monthly:2026:1:success",
        TransferStats.MonthStatus.class)).thenReturn(Future.succeededFuture(List.of()));
    when(repository.getMonthlyTransferStatus(any(MonthStatusTransfer.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    var req = new MonthStatusTransfer(2026, 1, "success");
    service.getMonthlyTransferStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalCount()).isEqualTo(10L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(1_000_000L);
          verify(repository).getMonthlyTransferStatus(any(MonthStatusTransfer.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyTransferStatus ─── */

  @Test
  @DisplayName("getYearlyTransferStatus cache hit")
  void getYearlyTransferStatusCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new TransferStats.YearStatus("2026", 120L, 12_000_000L));

    when(redis.getJsonList("transfer:stats:status:yearly:2026:success",
        TransferStats.YearStatus.class)).thenReturn(Future.succeededFuture(cached));

    var req = new YearStatusTransferRequest(2026, "success");
    service.getYearlyTransferStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalCount()).isEqualTo(120L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(12_000_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyTransferStatus cache miss")
  void getYearlyTransferStatusCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransferStats.YearStatus("2026", 120L, 12_000_000L));

    when(redis.getJsonList("transfer:stats:status:yearly:2026:success",
        TransferStats.YearStatus.class)).thenReturn(Future.succeededFuture(List.of()));
    when(repository.getYearlyTransferStatus(any(YearStatusTransferRequest.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    var req = new YearStatusTransferRequest(2026, "success");
    service.getYearlyTransferStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalCount()).isEqualTo(120L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(12_000_000L);
          verify(repository).getYearlyTransferStatus(any(YearStatusTransferRequest.class));
          ctx.completeNow();
        })));
  }
}
