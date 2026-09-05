package io.example.merchant.service.impl;

import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsTotalAmountRepository;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.merchant.Merchant.FindYearMerchant;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MerchantStatsTotalAmountServiceImplTest {

  @Mock
  private MerchantStatsTotalAmountRepository repo;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics tracingMetrics;

  private MerchantStatsTotalAmountServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new MerchantStatsTotalAmountServiceImpl(repo, redis, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(io.opentelemetry.context.Context.root(), java.time.Instant.now());
    when(tracingMetrics.startSpan(any(String.class))).thenReturn(tc);
    when(tracingMetrics.startSpan(any(String.class), any())).thenReturn(tc);
  }

  @Test
  @DisplayName("getMonthlyTotalAmounts cache hit")
  void getMonthlyTotalAmountsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"month":"Jan","amount":100}]
        """;
    when(redis.get("stats:totalamount:global:monthly:2026")).thenReturn(Future.succeededFuture(json));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    service.getMonthlyTotalAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getAmount()).isEqualTo(100L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyTotalAmounts cache miss")
  void getMonthlyTotalAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    List<MerchantStats.MonthAmount> data = List.of(new MerchantStats.MonthAmount("Jan", 100L));
    when(redis.get("stats:totalamount:global:monthly:2026")).thenReturn(Future.succeededFuture(null));
    when(repo.getMonthlyTotalAmountMerchant(2026)).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    service.getMonthlyTotalAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyTotalAmounts cache miss")
  void getYearlyTotalAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    List<MerchantStats.YearAmount> data = List.of(new MerchantStats.YearAmount("2026", 1000L));
    when(redis.get("stats:totalamount:global:yearly:2026")).thenReturn(Future.succeededFuture(null));
    when(repo.getYearlyTotalAmountMerchant(2026)).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    service.getYearlyTotalAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          ctx.completeNow();
        })));
  }
}
