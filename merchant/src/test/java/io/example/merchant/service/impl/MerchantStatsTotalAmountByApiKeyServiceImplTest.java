package io.example.merchant.service.impl;

import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.merchant.MonthYearTotalAmountApiKey;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsTotalAmountByApiKeyRepository;
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
class MerchantStatsTotalAmountByApiKeyServiceImplTest {
  @Mock private MerchantStatsTotalAmountByApiKeyRepository repo;
  @Mock private RedisService redis;
  @Mock private TracingMetrics metrics;
  private MerchantStatsTotalAmountByApiKeyServiceImpl service;
  @BeforeEach void setUp() { service = new MerchantStatsTotalAmountByApiKeyServiceImpl(repo, redis, metrics); }
  void mt() { var tc = new TracingContext(Context.root(), Instant.now()); when(metrics.startSpan(anyString())).thenReturn(tc); when(metrics.startSpan(anyString(), any())).thenReturn(tc); }

  @Test void getMonthlyTotalAmounts(VertxTestContext ctx) {
    mt(); var r = MonthYearTotalAmountApiKey.builder().apikey("key123").year(2026).build();
    when(redis.get("stats:totalamount:apikey:monthly:key123:2026")).thenReturn(Future.succeededFuture(null));
    when(repo.getMonthlyTotalAmountByApikey(r)).thenReturn(Future.succeededFuture(List.of(new MerchantStats.MonthAmount("Jan", 500L))));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));
    service.getMonthlyTotalAmounts(r).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); })));
  }

  @Test void getYearlyTotalAmounts(VertxTestContext ctx) {
    mt(); var r = MonthYearTotalAmountApiKey.builder().apikey("key123").year(2026).build();
    when(redis.get("stats:totalamount:apikey:yearly:key123:2026")).thenReturn(Future.succeededFuture(null));
    when(repo.getYearlyTotalAmountByApikey(r)).thenReturn(Future.succeededFuture(List.of(new MerchantStats.YearAmount("2025", 6000L))));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));
    service.getYearlyTotalAmounts(r).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); })));
  }
}
