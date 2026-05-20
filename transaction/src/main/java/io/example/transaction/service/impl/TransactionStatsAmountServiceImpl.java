package io.example.transaction.service.impl;

import java.time.Duration;
import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.model.TransactionStats;
import io.example.transaction.repository.TransactionStatsAmountRepository;
import io.example.transaction.service.TransactionStatsAmountService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pb.transaction.Transaction.FindByYearCardNumberTransactionRequest;
import pb.transaction.Transaction.FindYearTransactionStatus;

public class TransactionStatsAmountServiceImpl implements TransactionStatsAmountService {
  private final TransactionStatsAmountRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "transaction:stats:amount:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public TransactionStatsAmountServiceImpl(
      TransactionStatsAmountRepository repository,
      RedisService redis,
      TracingMetrics metrics) {
    this.repository = repository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<List<TransactionStats.MonthAmount>> getMonthlyAmounts(FindYearTransactionStatus request) {
    String cacheKey = CACHE_PREFIX + "monthly:" + request.getYear();
    var ctx = metrics.startSpan("TransactionStatsAmountService.getMonthlyAmounts");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.MonthAmount> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.MonthAmount.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyAmounts", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyAmounts(request)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyAmounts", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyAmounts", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransactionStats.YearAmount>> getYearlyAmounts(FindYearTransactionStatus request) {
    String cacheKey = CACHE_PREFIX + "yearly:" + request.getYear();
    var ctx = metrics.startSpan("TransactionStatsAmountService.getYearlyAmounts");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.YearAmount> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.YearAmount.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyAmounts", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getYearlyAmounts(request)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyAmounts", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyAmounts", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransactionStats.MonthAmount>> getMonthlyAmountsByCard(
      FindByYearCardNumberTransactionRequest request) {
    String cacheKey = CACHE_PREFIX + "monthly:card:" + request.getCardNumber() + ":" + request.getYear();
    var ctx = metrics.startSpan("TransactionStatsAmountService.getMonthlyAmountsByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.MonthAmount> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.MonthAmount.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyAmountsByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyAmountsByCard(request)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyAmountsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyAmountsByCard", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransactionStats.YearAmount>> getYearlyAmountsByCard(
      FindByYearCardNumberTransactionRequest request) {
    String cacheKey = CACHE_PREFIX + "yearly:card:" + request.getCardNumber() + ":" + request.getYear();
    var ctx = metrics.startSpan("TransactionStatsAmountService.getYearlyAmountsByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionStats.YearAmount> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionStats.YearAmount.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyAmountsByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getYearlyAmountsByCard(request)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyAmountsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyAmountsByCard", e.getMessage()));
        });
  }
}
