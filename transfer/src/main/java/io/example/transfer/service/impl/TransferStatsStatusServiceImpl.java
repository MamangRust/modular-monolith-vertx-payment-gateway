package io.example.transfer.service.impl;

import java.time.Duration;
import java.util.List;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transfer.model.TransferStats;
import io.example.transfer.repository.TransferStatsStatusRepository;
import io.example.transfer.service.TransferStatsStatusService;
import io.vertx.core.Future;

public class TransferStatsStatusServiceImpl implements TransferStatsStatusService {
  private final TransferStatsStatusRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);
  private static final String CACHE_PREFIX = "transfer:stats:status:";

  public TransferStatsStatusServiceImpl(TransferStatsStatusRepository repository, RedisService redis,
      TracingMetrics metrics) {
    this.repository = repository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<List<TransferStats.MonthStatus>> getMonthlyTransferStatus(
      pb.transfer.Transfer.FindMonthlyTransferStatus req, String status) {
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getYear() + ":" + req.getMonth() + ":" + status;
    var ctx = metrics.startSpan("TransferStatsStatusService.getMonthlyTransferStatus");

    return redis.getJsonList(cacheKey, TransferStats.MonthStatus.class)
        .compose(cached -> {
          if (!cached.isEmpty()) {
            metrics.completeSpanSuccess(ctx, "getMonthlyTransferStatus", "Success (from cache)");
            return Future.succeededFuture(cached);
          }
          return repository.getMonthlyTransferStatus(req, status)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransferStatus", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransferStatus", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransferStats.YearStatus>> getYearlyTransferStatus(
      pb.transfer.Transfer.FindYearTransferStatus req, String status) {
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getYear() + ":" + status;
    var ctx = metrics.startSpan("TransferStatsStatusService.getYearlyTransferStatus");

    return redis.getJsonList(cacheKey, TransferStats.YearStatus.class)
        .compose(cached -> {
          if (!cached.isEmpty()) {
            metrics.completeSpanSuccess(ctx, "getYearlyTransferStatus", "Success (from cache)");
            return Future.succeededFuture(cached);
          }
          return repository.getYearlyTransferStatus(req, status)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransferStatus", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransferStatus", e.getMessage()));
        });
  }
}
