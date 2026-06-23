package io.example.transfer.service.impl;

import java.time.Duration;
import java.util.List;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transfer.model.TransferStats;
import io.example.transfer.repository.TransferStatsAmountRepository;
import io.example.transfer.service.TransferStatsAmountService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransferStatsAmountServiceImpl implements TransferStatsAmountService {
  private final TransferStatsAmountRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);
  private static final String CACHE_PREFIX = "transfer:stats:amount:";

  @Override
  public Future<List<TransferStats.MonthAmount>> getMonthlyTransferAmounts(int year) {
    String cacheKey = CACHE_PREFIX + "monthly:" + year;
    var ctx = metrics.startSpan("TransferStatsAmountService.getMonthlyTransferAmounts");

    return redis.getJsonList(cacheKey, TransferStats.MonthAmount.class)
        .compose(cached -> {
          if (!cached.isEmpty()) {
            return Future.succeededFuture(cached);
          }
          return repository.getMonthlyTransferAmounts(year)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransferAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransferAmounts", e.getMessage()));
  }

  @Override
  public Future<List<TransferStats.YearAmount>> getYearlyTransferAmounts(int endYear) {
    String cacheKey = CACHE_PREFIX + "yearly:" + endYear;
    var ctx = metrics.startSpan("TransferStatsAmountService.getYearlyTransferAmounts");

    return redis.getJsonList(cacheKey, TransferStats.YearAmount.class)
        .compose(cached -> {
          if (!cached.isEmpty()) {
            return Future.succeededFuture(cached);
          }
          return repository.getYearlyTransferAmounts(endYear)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransferAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransferAmounts", e.getMessage()));
  }
}