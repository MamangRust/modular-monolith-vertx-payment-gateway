package io.example.saldo.service.impl;

import java.time.Duration;
import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.saldo.model.SaldoStats;
import io.example.saldo.repository.SaldoStatsTotalRepository;
import io.example.saldo.service.SaldoStatsTotalService;
import io.vertx.core.Future;
import io.example.saldo.domain.requests.MonthTotalSaldoBalance;

public class SaldoStatsTotalServiceImpl implements SaldoStatsTotalService {
  private final SaldoStatsTotalRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);
  private static final String CACHE_PREFIX = "saldo:stats:total:";

  public SaldoStatsTotalServiceImpl(SaldoStatsTotalRepository repository, RedisService redis, TracingMetrics metrics) {
    this.repository = repository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<List<SaldoStats.MonthTotalBalance>> getMonthlyTotalSaldoBalance(MonthTotalSaldoBalance req) {
    var ctx = metrics.startSpan("SaldoStatsTotalService.getMonthlyTotalSaldoBalance");
    String cacheKey = CACHE_PREFIX + "month:" + req.getYear() + ":" + req.getMonth();

    return redis.getJsonList(cacheKey, SaldoStats.MonthTotalBalance.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached);
          }
          return repository.getMonthlyTotalSaldoBalance(req)
              .compose(db -> redis.setJsonList(cacheKey, db, CACHE_TTL).map(db));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTotalSaldoBalance", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTotalSaldoBalance", e.getMessage()));
  }

  @Override
  public Future<List<SaldoStats.YearTotalBalance>> getYearlyTotalSaldoBalances(Integer year) {
    var ctx = metrics.startSpan("SaldoStatsTotalService.getYearlyTotalSaldoBalances");
    String cacheKey = CACHE_PREFIX + "year:" + year;

    return redis.getJsonList(cacheKey, SaldoStats.YearTotalBalance.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached);
          }
          return repository.getYearlyTotalSaldoBalances(year)
              .compose(db -> redis.setJsonList(cacheKey, db, CACHE_TTL).map(db));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTotalSaldoBalances", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTotalSaldoBalances", e.getMessage()));
  }
}
