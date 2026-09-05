package io.example.transfer.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.transfer.domain.requests.MonthStatusTransferCardNumber;
import io.example.transfer.domain.requests.MonthYearCardNumber;
import io.example.transfer.domain.requests.YearStatusTransferCardNumber;
import io.example.transfer.model.TransferStats;
import io.example.transfer.repository.TransferStatsByCardAmountReceiverRepository;
import io.example.transfer.repository.TransferStatsByCardAmountSenderRepository;
import io.example.transfer.repository.TransferStatsByCardStatusRepository;
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
class TransferStatsByCardServiceImplTest {

  @Mock
  private TransferStatsByCardAmountSenderRepository senderRepo;

  @Mock
  private TransferStatsByCardAmountReceiverRepository receiverRepo;

  @Mock
  private TransferStatsByCardStatusRepository statusRepo;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private TransferStatsByCardServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TransferStatsByCardServiceImpl(senderRepo, receiverRepo, statusRepo, redis, metrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(metrics.startSpan(anyString(), any())).thenReturn(tc);
  }

  /* ─── getMonthlySenderAmountsByCard ─── */

  @Test
  @DisplayName("getMonthlySenderAmountsByCard cache hit")
  void getMonthlySenderAmountsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new TransferStats.MonthAmount("January", 300_000L));

    when(redis.getJsonList("transfer:stats:card:sender:monthly:4111111111111111:2026",
        TransferStats.MonthAmount.class)).thenReturn(Future.succeededFuture(cached));

    var req = new MonthYearCardNumber("4111111111111111", 2026);
    service.getMonthlySenderAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(300_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlySenderAmountsByCard cache miss")
  void getMonthlySenderAmountsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransferStats.MonthAmount("January", 300_000L));

    when(redis.getJsonList("transfer:stats:card:sender:monthly:4111111111111111:2026",
        TransferStats.MonthAmount.class)).thenReturn(Future.succeededFuture(List.of()));
    when(senderRepo.getMonthlySenderAmountsByCard(any(MonthYearCardNumber.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    var req = new MonthYearCardNumber("4111111111111111", 2026);
    service.getMonthlySenderAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(300_000L);
          verify(senderRepo).getMonthlySenderAmountsByCard(any(MonthYearCardNumber.class));
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyReceiverAmountsByCard ─── */

  @Test
  @DisplayName("getMonthlyReceiverAmountsByCard cache hit")
  void getMonthlyReceiverAmountsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new TransferStats.MonthAmount("January", 200_000L));

    when(redis.getJsonList("transfer:stats:card:receiver:monthly:5111111111111111:2026",
        TransferStats.MonthAmount.class)).thenReturn(Future.succeededFuture(cached));

    var req = new MonthYearCardNumber("5111111111111111", 2026);
    service.getMonthlyReceiverAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(200_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyReceiverAmountsByCard cache miss")
  void getMonthlyReceiverAmountsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransferStats.MonthAmount("January", 200_000L));

    when(redis.getJsonList("transfer:stats:card:receiver:monthly:5111111111111111:2026",
        TransferStats.MonthAmount.class)).thenReturn(Future.succeededFuture(List.of()));
    when(receiverRepo.getMonthlyReceiverAmountsByCard(any(MonthYearCardNumber.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    var req = new MonthYearCardNumber("5111111111111111", 2026);
    service.getMonthlyReceiverAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(200_000L);
          verify(receiverRepo).getMonthlyReceiverAmountsByCard(any(MonthYearCardNumber.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlySenderAmountsByCard ─── */

  @Test
  @DisplayName("getYearlySenderAmountsByCard cache hit")
  void getYearlySenderAmountsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new TransferStats.YearAmount("2026", 3_600_000L));

    when(redis.getJsonList("transfer:stats:card:sender:yearly:4111111111111111:2026",
        TransferStats.YearAmount.class)).thenReturn(Future.succeededFuture(cached));

    var req = new MonthYearCardNumber("4111111111111111", 2026);
    service.getYearlySenderAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(3_600_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlySenderAmountsByCard cache miss")
  void getYearlySenderAmountsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransferStats.YearAmount("2026", 3_600_000L));

    when(redis.getJsonList("transfer:stats:card:sender:yearly:4111111111111111:2026",
        TransferStats.YearAmount.class)).thenReturn(Future.succeededFuture(List.of()));
    when(senderRepo.getYearlySenderAmountsByCard(any(MonthYearCardNumber.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    var req = new MonthYearCardNumber("4111111111111111", 2026);
    service.getYearlySenderAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(3_600_000L);
          verify(senderRepo).getYearlySenderAmountsByCard(any(MonthYearCardNumber.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyReceiverAmountsByCard ─── */

  @Test
  @DisplayName("getYearlyReceiverAmountsByCard cache hit")
  void getYearlyReceiverAmountsByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new TransferStats.YearAmount("2026", 2_400_000L));

    when(redis.getJsonList("transfer:stats:card:receiver:yearly:5111111111111111:2026",
        TransferStats.YearAmount.class)).thenReturn(Future.succeededFuture(cached));

    var req = new MonthYearCardNumber("5111111111111111", 2026);
    service.getYearlyReceiverAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalAmount()).isEqualTo(2_400_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyReceiverAmountsByCard cache miss")
  void getYearlyReceiverAmountsByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransferStats.YearAmount("2026", 2_400_000L));

    when(redis.getJsonList("transfer:stats:card:receiver:yearly:5111111111111111:2026",
        TransferStats.YearAmount.class)).thenReturn(Future.succeededFuture(List.of()));
    when(receiverRepo.getYearlyReceiverAmountsByCard(any(MonthYearCardNumber.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    var req = new MonthYearCardNumber("5111111111111111", 2026);
    service.getYearlyReceiverAmountsByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(2_400_000L);
          verify(receiverRepo).getYearlyReceiverAmountsByCard(any(MonthYearCardNumber.class));
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyStatusByCard ─── */

  @Test
  @DisplayName("getMonthlyStatusByCard cache hit")
  void getMonthlyStatusByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new TransferStats.MonthStatus("2026", "January", 5L, 500_000L));

    when(redis.getJsonList(
        "transfer:stats:card:status:monthly:4111111111111111:2026:1:success",
        TransferStats.MonthStatus.class)).thenReturn(Future.succeededFuture(cached));

    var req = new MonthStatusTransferCardNumber("4111111111111111", 2026, 1, "success");
    service.getMonthlyStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getMonth()).isEqualTo("January");
          assertThat(res.get(0).getTotalCount()).isEqualTo(5L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(500_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyStatusByCard cache miss")
  void getMonthlyStatusByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransferStats.MonthStatus("2026", "January", 5L, 500_000L));

    when(redis.getJsonList(
        "transfer:stats:card:status:monthly:4111111111111111:2026:1:success",
        TransferStats.MonthStatus.class)).thenReturn(Future.succeededFuture(List.of()));
    when(statusRepo.getMonthlyStatusByCard(any(MonthStatusTransferCardNumber.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    var req = new MonthStatusTransferCardNumber("4111111111111111", 2026, 1, "success");
    service.getMonthlyStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalCount()).isEqualTo(5L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(500_000L);
          verify(statusRepo).getMonthlyStatusByCard(any(MonthStatusTransferCardNumber.class));
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyStatusByCard ─── */

  @Test
  @DisplayName("getYearlyStatusByCard cache hit")
  void getYearlyStatusByCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cached = List.of(new TransferStats.YearStatus("2026", 60L, 6_000_000L));

    when(redis.getJsonList(
        "transfer:stats:card:status:yearly:4111111111111111:2026:success",
        TransferStats.YearStatus.class)).thenReturn(Future.succeededFuture(cached));

    var req = new YearStatusTransferCardNumber("4111111111111111", 2026, "success");
    service.getYearlyStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getYear()).isEqualTo("2026");
          assertThat(res.get(0).getTotalCount()).isEqualTo(60L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyStatusByCard cache miss")
  void getYearlyStatusByCardCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var data = List.of(new TransferStats.YearStatus("2026", 60L, 6_000_000L));

    when(redis.getJsonList(
        "transfer:stats:card:status:yearly:4111111111111111:2026:success",
        TransferStats.YearStatus.class)).thenReturn(Future.succeededFuture(List.of()));
    when(statusRepo.getYearlyStatusByCard(any(YearStatusTransferCardNumber.class)))
        .thenReturn(Future.succeededFuture(data));
    when(redis.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    var req = new YearStatusTransferCardNumber("4111111111111111", 2026, "success");
    service.getYearlyStatusByCard(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getTotalCount()).isEqualTo(60L);
          assertThat(res.get(0).getTotalAmount()).isEqualTo(6_000_000L);
          verify(statusRepo).getYearlyStatusByCard(any(YearStatusTransferCardNumber.class));
          ctx.completeNow();
        })));
  }
}
