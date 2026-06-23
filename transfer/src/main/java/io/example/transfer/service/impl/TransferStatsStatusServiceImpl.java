package io.example.transfer.service.impl;

import java.time.Duration;
import java.util.List;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transfer.domain.requests.MonthStatusTransfer;
import io.example.transfer.domain.requests.YearStatusTransferRequest;
import io.example.transfer.model.TransferStats;
import io.example.transfer.repository.TransferStatsStatusRepository;
import io.example.transfer.service.TransferStatsStatusService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransferStatsStatusServiceImpl implements TransferStatsStatusService {
  private final TransferStatsStatusRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);
  private static final String CACHE_PREFIX = "transfer:stats:status:";

  @Override
  public Future<List<TransferStats.MonthStatus>> getMonthlyTransferStatus(MonthStatusTransfer req) {
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getYear() + ":" + req.getMonth() + ":" + req.getStatus();
    var ctx = metrics.startSpan("TransferStatsStatusService.getMonthlyTransferStatus");

    return redis.getJsonList(cacheKey, TransferStats.MonthStatus.class)
        .compose(cached -> {
          if (!cached.isEmpty())
            return Future.succeededFuture(cached);
          return repository.getMonthlyTransferStatus(req)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransferStatus", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransferStatus", e.getMessage()));
  }

  @Override
  public Future<List<TransferStats.YearStatus>> getYearlyTransferStatus(YearStatusTransferRequest req) {
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getYear() + ":" + req.getStatus();
    var ctx = metrics.startSpan("TransferStatsStatusService.getYearlyTransferStatus");

    return redis.getJsonList(cacheKey, TransferStats.YearStatus.class)
        .compose(cached -> {
          if (!cached.isEmpty())
            return Future.succeededFuture(cached);
          return repository.getYearlyTransferStatus(req)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransferStatus", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransferStatus", e.getMessage()));
  }
}