package io.example.topup.service.impl;

import java.time.Duration;
import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.topup.model.TopupStats;
import io.example.topup.repository.TopupStatsAmountRepository;
import io.example.topup.repository.TopupStatsByCardAmountRepository;
import io.example.topup.service.TopupStatsAmountService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pb.topup.Topup.FindYearTopupCardNumber;
import pb.topup.Topup.FindYearTopupStatus;

public class TopupStatsAmountServiceImpl implements TopupStatsAmountService {
  private final TopupStatsAmountRepository repository;
  private final TopupStatsByCardAmountRepository cardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "topup:stats:amount:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public TopupStatsAmountServiceImpl(
      TopupStatsAmountRepository repository,
      TopupStatsByCardAmountRepository cardRepository,
      RedisService redis,
      TracingMetrics metrics) {
    this.repository = repository;
    this.cardRepository = cardRepository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmounts(FindYearTopupStatus req) {
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsAmountService.getMonthlyTopupAmounts");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.MonthAmount> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.MonthAmount.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyTopupAmounts", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyTopupAmounts(req)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupAmounts", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupAmounts", e.getMessage()));
        });
  }

  @Override
  public Future<List<TopupStats.YearAmount>> getYearlyTopupAmounts(FindYearTopupStatus req) {
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsAmountService.getYearlyTopupAmounts");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.YearAmount> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.YearAmount.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyTopupAmounts", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getYearlyTopupAmounts(req)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupAmounts", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupAmounts", e.getMessage()));
        });
  }

  @Override
  public Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmountsByCard(FindYearTopupCardNumber req) {
    String cacheKey = CACHE_PREFIX + "monthly:card:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsAmountService.getMonthlyTopupAmountsByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.MonthAmount> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.MonthAmount.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyTopupAmountsByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return cardRepository.getMonthlyTopupAmountsByCard(req)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupAmountsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupAmountsByCard", e.getMessage()));
        });
  }

  @Override
  public Future<List<TopupStats.YearAmount>> getYearlyTopupAmountsByCard(FindYearTopupCardNumber req) {
    String cacheKey = CACHE_PREFIX + "yearly:card:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsAmountService.getYearlyTopupAmountsByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.YearAmount> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.YearAmount.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyTopupAmountsByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return cardRepository.getYearlyTopupAmountsByCard(req)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupAmountsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupAmountsByCard", e.getMessage()));
        });
  }
}
