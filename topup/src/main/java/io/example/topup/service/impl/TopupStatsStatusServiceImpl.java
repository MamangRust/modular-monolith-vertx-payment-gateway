package io.example.topup.service.impl;

import java.time.Duration;
import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.topup.model.TopupStats;
import io.example.topup.repository.TopupStatsByCardStatusRepository;
import io.example.topup.repository.TopupStatsStatusRepository;
import io.example.topup.service.TopupStatsStatusService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pb.topup.Topup.FindMonthlyTopupStatus;
import pb.topup.Topup.FindMonthlyTopupStatusCardNumber;
import pb.topup.Topup.FindYearTopupStatus;
import pb.topup.Topup.FindYearTopupStatusCardNumber;

public class TopupStatsStatusServiceImpl implements TopupStatsStatusService {
  private final TopupStatsStatusRepository repository;
  private final TopupStatsByCardStatusRepository cardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "topup:stats:status:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public TopupStatsStatusServiceImpl(
      TopupStatsStatusRepository repository,
      TopupStatsByCardStatusRepository cardRepository,
      RedisService redis,
      TracingMetrics metrics) {
    this.repository = repository;
    this.cardRepository = cardRepository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatus(FindMonthlyTopupStatus req, String status) {
    String cacheKey = CACHE_PREFIX + "monthly:" + status + ":" + req.getYear() + ":" + req.getMonth();
    var ctx = metrics.startSpan("TopupStatsStatusService.getMonthlyTopupStatus");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.MonthStatus> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.MonthStatus.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyTopupStatus", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyTopupStatus(req, status)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupStatus", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupStatus", e.getMessage()));
        });
  }

  @Override
  public Future<List<TopupStats.YearStatus>> getYearlyTopupStatus(FindYearTopupStatus req, String status) {
    String cacheKey = CACHE_PREFIX + "yearly:" + status + ":" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsStatusService.getYearlyTopupStatus");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.YearStatus> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.YearStatus.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyTopupStatus", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return repository.getYearlyTopupStatus(req, status)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupStatus", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupStatus", e.getMessage()));
        });
  }

  @Override
  public Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatusByCard(FindMonthlyTopupStatusCardNumber req,
      String status) {
    String cacheKey = CACHE_PREFIX + "monthly:card:" + status + ":" + req.getCardNumber() + ":" + req.getYear() + ":"
        + req.getMonth();
    var ctx = metrics.startSpan("TopupStatsStatusService.getMonthlyTopupStatusByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.MonthStatus> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.MonthStatus.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getMonthlyTopupStatusByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return cardRepository.getMonthlyTopupStatusByCard(req, status)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupStatusByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupStatusByCard", e.getMessage()));
        });
  }

  @Override
  public Future<List<TopupStats.YearStatus>> getYearlyTopupStatusByCard(FindYearTopupStatusCardNumber req,
      String status) {
    String cacheKey = CACHE_PREFIX + "yearly:card:" + status + ":" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsStatusService.getYearlyTopupStatusByCard");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TopupStats.YearStatus> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TopupStats.YearStatus.class))
                .toList();
            metrics.completeSpanSuccess(ctx, "getYearlyTopupStatusByCard", "Success (from cache)");
            return Future.succeededFuture(list);
          }
          return cardRepository.getYearlyTopupStatusByCard(req, status)
              .compose(res -> redis.setJson(cacheKey, new JsonArray(res), CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupStatusByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupStatusByCard", e.getMessage()));
        });
  }
}
