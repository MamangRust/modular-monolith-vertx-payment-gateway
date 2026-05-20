package io.example.topup.service.impl;

import java.time.Duration;
import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.topup.model.TopupStats;
import io.example.topup.repository.TopupStatsByCardMethodRepository;
import io.example.topup.repository.TopupStatsMethodRepository;
import io.example.topup.service.TopupStatsMethodService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pb.topup.Topup.FindYearTopupCardNumber;
import pb.topup.Topup.FindYearTopupStatus;

public class TopupStatsMethodServiceImpl implements TopupStatsMethodService {
  private final TopupStatsMethodRepository repository;
  private final TopupStatsByCardMethodRepository cardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "topup:stats:method:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public TopupStatsMethodServiceImpl(
      TopupStatsMethodRepository repository,
      TopupStatsByCardMethodRepository cardRepository,
      RedisService redis,
      TracingMetrics metrics) {
    this.repository = repository;
    this.cardRepository = cardRepository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethods(FindYearTopupStatus req) {
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsMethodService.getMonthlyTopupMethods");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.MonthMethod> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.MonthMethod.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyTopupMethods", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyTopupMethods(req)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupMethods", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupMethods", e.getMessage()));
        });
  }

  @Override
  public Future<List<TopupStats.YearMethod>> getYearlyTopupMethods(FindYearTopupStatus req) {
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsMethodService.getYearlyTopupMethods");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.YearMethod> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.YearMethod.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyTopupMethods", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getYearlyTopupMethods(req)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupMethods", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupMethods", e.getMessage()));
        });
  }

  @Override
  public Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethodsByCard(FindYearTopupCardNumber req) {
    String cacheKey = CACHE_PREFIX + "monthly:card:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsMethodService.getMonthlyTopupMethodsByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.MonthMethod> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.MonthMethod.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyTopupMethodsByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return cardRepository.getMonthlyTopupMethodsByCard(req)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupMethodsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupMethodsByCard", e.getMessage()));
        });
  }

  @Override
  public Future<List<TopupStats.YearMethod>> getYearlyTopupMethodsByCard(FindYearTopupCardNumber req) {
    String cacheKey = CACHE_PREFIX + "yearly:card:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsMethodService.getYearlyTopupMethodsByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.YearMethod> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.YearMethod.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyTopupMethodsByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return cardRepository.getYearlyTopupMethodsByCard(req)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupMethodsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupMethodsByCard", e.getMessage()));
        });
  }
}
