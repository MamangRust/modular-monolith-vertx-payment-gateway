package io.example.saldo.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.saldo.domain.requests.MonthTotalSaldoBalance;
import io.example.saldo.model.SaldoStats;
import io.example.saldo.repository.SaldoStatsTotalRepository;
import io.example.saldo.service.SaldoStatsTotalService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SaldoStatsTotalServiceImpl implements SaldoStatsTotalService {
  private final SaldoStatsTotalRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);
  private static final String CACHE_PREFIX = "saldo:stats:total:";

  @Override
  public Future<List<SaldoStats.MonthTotalBalance>> getMonthlyTotalSaldoBalance(MonthTotalSaldoBalance req) {
    var ctx = metrics.startSpan("SaldoStatsTotalService.getMonthlyTotalSaldoBalance");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "month:" + req.getYear() + ":" + req.getMonth();

    return redis.getJsonList(cacheKey, SaldoStats.MonthTotalBalance.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            span.setAttribute("saldo.cache_hit", true);
            return Future.succeededFuture(cached);
          }
          span.setAttribute("saldo.cache_hit", false);
          return repository.getMonthlyTotalSaldoBalance(req)
              .compose(db -> redis.setJsonList(cacheKey, db, CACHE_TTL).map(db));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTotalSaldoBalance", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTotalSaldoBalance", e.getMessage()));
  }

  @Override
  public Future<List<SaldoStats.YearTotalBalance>> getYearlyTotalSaldoBalances(Integer year) {
    var ctx = metrics.startSpan("SaldoStatsTotalService.getYearlyTotalSaldoBalances");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "year:" + year;

    return redis.getJsonList(cacheKey, SaldoStats.YearTotalBalance.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            span.setAttribute("saldo.cache_hit", true);
            return Future.succeededFuture(cached);
          }
          span.setAttribute("saldo.cache_hit", false);
          return repository.getYearlyTotalSaldoBalances(year)
              .compose(db -> redis.setJsonList(cacheKey, db, CACHE_TTL).map(db));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTotalSaldoBalances", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTotalSaldoBalances", e.getMessage()));
  }
}