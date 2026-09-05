package io.example.merchant.service.impl;

import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsMethodRepository;
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
class MerchantStatsMethodServiceImplTest {

  @Mock
  private MerchantStatsMethodRepository repo;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics tracingMetrics;

  private MerchantStatsMethodServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new MerchantStatsMethodServiceImpl(repo, redis, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(io.opentelemetry.context.Context.root(), java.time.Instant.now());
    when(tracingMetrics.startSpan(any(String.class))).thenReturn(tc);
    when(tracingMetrics.startSpan(any(String.class), any())).thenReturn(tc);
  }

  @Test
  @DisplayName("getMonthlyMethodAmounts cache hit")
  void getMonthlyMethodAmountsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        [{"month":"Jan","paymentMethod":"CREDIT","amount":100}]
        """;
    when(redis.get("stats:method:global:monthly:2026")).thenReturn(Future.succeededFuture(json));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    service.getMonthlyMethodAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getAmount()).isEqualTo(100L);
          assertThat(res.get(0).getPaymentMethod()).isEqualTo("CREDIT");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyMethodAmounts cache miss")
  void getMonthlyMethodAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    List<MerchantStats.MonthMethod> data = List.of(new MerchantStats.MonthMethod("Jan", "CREDIT", 100L));
    when(redis.get("stats:method:global:monthly:2026")).thenReturn(Future.succeededFuture(null));
    when(repo.getMonthlyPaymentMethodsMerchant(2026)).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    service.getMonthlyMethodAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyMethodAmounts cache miss")
  void getYearlyMethodAmountsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    List<MerchantStats.YearMethod> data = List.of(new MerchantStats.YearMethod("2026", "CREDIT", 1000L));
    when(redis.get("stats:method:global:yearly:2026")).thenReturn(Future.succeededFuture(null));
    when(repo.getYearlyPaymentMethodMerchant(2026)).thenReturn(Future.succeededFuture(data));
    when(redis.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    service.getYearlyMethodAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          ctx.completeNow();
        })));
  }
}
