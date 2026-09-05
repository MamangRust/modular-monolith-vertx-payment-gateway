package io.example.merchant.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.merchant.MonthYearPaymentMethodApiKey;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsMethodByApiKeyRepository;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test
;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@SuppressWarnings("unchecked")
class MerchantStatsMethodByApiKeyServiceImplTest {
  @Mock private MerchantStatsMethodByApiKeyRepository repo;
  @Mock private RedisService redis;
  @Mock private TracingMetrics metrics;
  private MerchantStatsMethodByApiKeyServiceImpl service;
  @BeforeEach void setUp() { service = new MerchantStatsMethodByApiKeyServiceImpl(repo, redis, metrics); }
  void mt() { var tc = new TracingContext(Context.root(), Instant.now()); when(metrics.startSpan(anyString())).thenReturn(tc); when(metrics.startSpan(anyString(), any())).thenReturn(tc); }

  @Test void getMonthlyMethodAmounts(VertxTestContext ctx) {
    mt(); var r = MonthYearPaymentMethodApiKey.builder().apikey("key123").year(2026).build();
    when(redis.get("stats:method:apikey:monthly:key123:2026")).thenReturn((Future) Future.succeededFuture(null));
    when(repo.getMonthlyPaymentMethodByApikey(r)).thenReturn((Future) Future.succeededFuture(List.of(new MerchantStats.MonthMethod("Jan", "CC", 500L))));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn((Future) Future.succeededFuture("OK"));
    service.getMonthlyMethodAmounts(r).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); })));
  }

  @Test void getYearlyMethodAmounts(VertxTestContext ctx) {
    mt(); var r = MonthYearPaymentMethodApiKey.builder().apikey("key123").year(2026).build();
    when(redis.get("stats:method:apikey:yearly:key123:2026")).thenReturn((Future) Future.succeededFuture(null));
    when(repo.getYearlyPaymentMethodByApikey(r)).thenReturn((Future) Future.succeededFuture(List.of(new MerchantStats.YearMethod("2025", "CC", 6000L))));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn((Future) Future.succeededFuture("OK"));
    service.getYearlyMethodAmounts(r).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); })));
  }
}
