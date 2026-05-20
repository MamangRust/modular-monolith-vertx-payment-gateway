package io.example.saldo.service.impl;

import java.time.Duration;
import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.saldo.model.SaldoStats;
import io.example.saldo.repository.SaldoStatsBalanceRepository;
import io.example.saldo.service.SaldoStatsBalanceService;
import io.vertx.core.Future;

public class SaldoStatsBalanceServiceImpl implements SaldoStatsBalanceService {
  private final SaldoStatsBalanceRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);
  private static final String CACHE_PREFIX = "saldo:stats:balance:";

  public SaldoStatsBalanceServiceImpl(SaldoStatsBalanceRepository repository, RedisService redis, TracingMetrics metrics) {
    this.repository = repository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<List<SaldoStats.MonthBalance>> getMonthlySaldoBalances(Integer year) {
    var ctx = metrics.startSpan("SaldoStatsBalanceService.getMonthlySaldoBalances");
    String cacheKey = CACHE_PREFIX + "month:" + year;

    return redis.getJsonList(cacheKey, SaldoStats.MonthBalance.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached);
          }
          return repository.getMonthlySaldoBalances(year)
              .compose(db -> redis.setJsonList(cacheKey, db, CACHE_TTL).map(db));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlySaldoBalances", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlySaldoBalances", e.getMessage()));
  }

  @Override
  public Future<List<SaldoStats.YearBalance>> getYearlySaldoBalances(Integer year) {
    var ctx = metrics.startSpan("SaldoStatsBalanceService.getYearlySaldoBalances");
    String cacheKey = CACHE_PREFIX + "year:" + year;

    return redis.getJsonList(cacheKey, SaldoStats.YearBalance.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached);
          }
          return repository.getYearlySaldoBalances(year)
              .compose(db -> redis.setJsonList(cacheKey, db, CACHE_TTL).map(db));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlySaldoBalances", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlySaldoBalances", e.getMessage()));
  }
}
