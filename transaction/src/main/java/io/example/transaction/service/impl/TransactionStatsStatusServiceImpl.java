package io.example.transaction.service.impl;

import java.time.Duration;
import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.model.TransactionStats;
import io.example.transaction.repository.TransactionStatsStatusRepository;
import io.example.transaction.service.TransactionStatsStatusService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pb.transaction.Transaction.FindMonthlyTransactionStatus;
import pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber;
import pb.transaction.Transaction.FindYearTransactionStatus;
import pb.transaction.Transaction.FindYearTransactionStatusCardNumber;

public class TransactionStatsStatusServiceImpl implements TransactionStatsStatusService {
  private final TransactionStatsStatusRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "transaction:stats:status:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public TransactionStatsStatusServiceImpl(
      TransactionStatsStatusRepository repository,
      RedisService redis,
      TracingMetrics metrics) {
    this.repository = repository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<List<TransactionStats.MonthStatus>> getMonthlyStatus(FindMonthlyTransactionStatus req, String status) {
    String cacheKey = CACHE_PREFIX + "monthly:" + status + ":" + req.getYear() + ":" + req.getMonth();
    var ctx = metrics.startSpan("TransactionStatsStatusService.getMonthlyStatus");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.MonthStatus> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.MonthStatus.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyStatus", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyStatus(req, status)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyStatus", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyStatus", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransactionStats.YearStatus>> getYearlyStatus(FindYearTransactionStatus req, String status) {
    String cacheKey = CACHE_PREFIX + "yearly:" + status + ":" + req.getYear();
    var ctx = metrics.startSpan("TransactionStatsStatusService.getYearlyStatus");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.YearStatus> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.YearStatus.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyStatus", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getYearlyStatus(req, status)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyStatus", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyStatus", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransactionStats.MonthStatus>> getMonthlyStatusByCard(FindMonthlyTransactionStatusCardNumber req,
      String status) {
    String cacheKey = CACHE_PREFIX + "monthly:card:" + status + ":" + req.getCardNumber() + ":" + req.getYear() + ":"
        + req.getMonth();
    var ctx = metrics.startSpan("TransactionStatsStatusService.getMonthlyStatusByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.MonthStatus> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.MonthStatus.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyStatusByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyStatusByCard(req, status)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyStatusByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyStatusByCard", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransactionStats.YearStatus>> getYearlyStatusByCard(FindYearTransactionStatusCardNumber req,
      String status) {
    String cacheKey = CACHE_PREFIX + "yearly:card:" + status + ":" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TransactionStatsStatusService.getYearlyStatusByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.YearStatus> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.YearStatus.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyStatusByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getYearlyStatusByCard(req, status)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyStatusByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyStatusByCard", e.getMessage()));
        });
  }
}
