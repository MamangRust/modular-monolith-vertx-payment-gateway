package io.example.merchant.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.merchant.*;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.*;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class MerchantStatsAmountByApiKeyServiceImplTest {
  @Mock private MerchantStatsAmountByApiKeyRepository repo;
  @Mock private RedisService redis;
  @Mock private TracingMetrics metrics;
  private MerchantStatsAmountByApiKeyServiceImpl service;
  @BeforeEach void setUp() { service = new MerchantStatsAmountByApiKeyServiceImpl(repo, redis, metrics); }
  void mt() { var tc = new TracingContext(Context.root(), Instant.now()); when(metrics.startSpan(anyString())).thenReturn(tc); when(metrics.startSpan(anyString(), any())).thenReturn(tc); }

  @Test void getMonthlyAmountsCacheHit(VertxTestContext ctx) {
    mt(); when(redis.get("stats:amount:apikey:monthly:key123:2026")).thenReturn(Future.succeededFuture("[{\"month\":\"Jan\",\"amount\":500}]"));
    service.getMonthlyAmounts(MonthYearAmountApiKey.builder().apikey("key123").year(2026).build())
        .onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); })));
  }

  @Test void getMonthlyAmountsCacheMiss(VertxTestContext ctx) {
    mt(); var r = MonthYearAmountApiKey.builder().apikey("key123").year(2026).build();
    when(redis.get("stats:amount:apikey:monthly:key123:2026")).thenReturn(Future.succeededFuture(null));
    when(repo.getMonthlyAmountByApikey(r)).thenReturn(Future.succeededFuture(List.of(new MerchantStats.MonthAmount("Jan", 500L))));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));
    service.getMonthlyAmounts(r).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); })));
  }

  @Test void getYearlyAmountsCacheMiss(VertxTestContext ctx) {
    mt(); var r = MonthYearAmountApiKey.builder().apikey("key123").year(2026).build();
    when(redis.get("stats:amount:apikey:yearly:key123:2026")).thenReturn(Future.succeededFuture(null));
    when(repo.getYearlyAmountByApikey(r)).thenReturn(Future.succeededFuture(List.of(new MerchantStats.YearAmount("2025", 6000L))));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));
    service.getYearlyAmounts(r).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); })));
  }
}
