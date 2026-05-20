package io.example.transaction.service.impl;

import java.time.Duration;
import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.model.TransactionStats;
import io.example.transaction.repository.TransactionStatsMethodRepository;
import io.example.transaction.service.TransactionStatsMethodService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pb.transaction.Transaction.FindByYearCardNumberTransactionRequest;
import pb.transaction.Transaction.FindYearTransactionStatus;

public class TransactionStatsMethodServiceImpl implements TransactionStatsMethodService {
  private final TransactionStatsMethodRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "transaction:stats:method:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public TransactionStatsMethodServiceImpl(
      TransactionStatsMethodRepository repository,
      RedisService redis,
      TracingMetrics metrics) {
    this.repository = repository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<List<TransactionStats.MonthMethod>> getMonthlyMethods(FindYearTransactionStatus request) {
    String cacheKey = CACHE_PREFIX + "monthly:" + request.getYear();
    var ctx = metrics.startSpan("TransactionStatsMethodService.getMonthlyMethods");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.MonthMethod> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.MonthMethod.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyMethods", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyMethods(request)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyMethods", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyMethods", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransactionStats.YearMethod>> getYearlyMethods(FindYearTransactionStatus request) {
    String cacheKey = CACHE_PREFIX + "yearly:" + request.getYear();
    var ctx = metrics.startSpan("TransactionStatsMethodService.getYearlyMethods");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.YearMethod> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.YearMethod.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyMethods", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getYearlyMethods(request)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyMethods", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyMethods", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransactionStats.MonthMethod>> getMonthlyMethodsByCard(
      FindByYearCardNumberTransactionRequest request) {
    String cacheKey = CACHE_PREFIX + "monthly:card:" + request.getCardNumber() + ":" + request.getYear();
    var ctx = metrics.startSpan("TransactionStatsMethodService.getMonthlyMethodsByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.MonthMethod> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.MonthMethod.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyMethodsByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyMethodsByCard(request)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyMethodsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyMethodsByCard", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransactionStats.YearMethod>> getYearlyMethodsByCard(
      FindByYearCardNumberTransactionRequest request) {
    String cacheKey = CACHE_PREFIX + "yearly:card:" + request.getCardNumber() + ":" + request.getYear();
    var ctx = metrics.startSpan("TransactionStatsMethodService.getYearlyMethodsByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.YearMethod> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.YearMethod.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyMethodsByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getYearlyMethodsByCard(request)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyMethodsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyMethodsByCard", e.getMessage()));
        });
  }
}
