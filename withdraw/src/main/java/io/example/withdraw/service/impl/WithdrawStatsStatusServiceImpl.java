package io.example.withdraw.service.impl;

import java.time.Duration;
import java.util.List;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.withdraw.model.WithdrawStats;
import io.example.withdraw.repository.WithdrawStatsStatusRepository;
import io.example.withdraw.service.WithdrawStatsStatusService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class WithdrawStatsStatusServiceImpl implements WithdrawStatsStatusService {
  private final WithdrawStatsStatusRepository repository;
  private final RedisService redisService;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "withdraw:stats:status:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  @Override
  public Future<List<WithdrawStats.MonthStatus>> getMonthlyWithdrawStatus(int year, int month, String status) {
    var ctx = metrics.startSpan("WithdrawStatsStatusService.getMonthlyWithdrawStatus");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "monthly:" + year + ":" + month + ":" + status;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            JsonArray arr = new JsonArray(cached);
            List<WithdrawStats.MonthStatus> data = arr.stream()
                .map(o -> ((JsonObject) o).mapTo(WithdrawStats.MonthStatus.class))
                .collect(Collectors.toList());
            return Future.succeededFuture(data);
          }
          span.setAttribute("cache.hit", false);
          return repository.getMonthlyWithdrawStatus(year, month, status)
              .map(list -> {
                redisService.setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()), CACHE_TTL);
                return list;
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyWithdrawStatus", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyWithdrawStatus", e.getMessage()));
  }

  @Override
  public Future<List<WithdrawStats.YearStatus>> getYearlyWithdrawStatus(int endYear, String status) {
    var ctx = metrics.startSpan("WithdrawStatsStatusService.getYearlyWithdrawStatus");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "yearly:" + endYear + ":" + status;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            JsonArray arr = new JsonArray(cached);
            List<WithdrawStats.YearStatus> data = arr.stream()
                .map(o -> ((JsonObject) o).mapTo(WithdrawStats.YearStatus.class))
                .collect(Collectors.toList());
            return Future.succeededFuture(data);
          }
          span.setAttribute("cache.hit", false);
          return repository.getYearlyWithdrawStatus(endYear, status)
              .map(list -> {
                redisService.setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()), CACHE_TTL);
                return list;
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyWithdrawStatus", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyWithdrawStatus", e.getMessage()));
  }

  @Override
  public Future<List<WithdrawStats.MonthStatus>> getMonthlyStatusByCard(String card, int year, int month, String status) {
    var ctx = metrics.startSpan("WithdrawStatsStatusService.getMonthlyStatusByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "card:monthly:" + card + ":" + year + ":" + month + ":" + status;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            JsonArray arr = new JsonArray(cached);
            List<WithdrawStats.MonthStatus> data = arr.stream()
                .map(o -> ((JsonObject) o).mapTo(WithdrawStats.MonthStatus.class))
                .collect(Collectors.toList());
            return Future.succeededFuture(data);
          }
          span.setAttribute("cache.hit", false);
          return repository.getMonthlyStatusByCard(card, year, month, status)
              .map(list -> {
                redisService.setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()), CACHE_TTL);
                return list;
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyStatusByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyStatusByCard", e.getMessage()));
  }

  @Override
  public Future<List<WithdrawStats.YearStatus>> getYearlyStatusByCard(String card, int year, String status) {
    var ctx = metrics.startSpan("WithdrawStatsStatusService.getYearlyStatusByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "card:yearly:" + card + ":" + year + ":" + status;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            JsonArray arr = new JsonArray(cached);
            List<WithdrawStats.YearStatus> data = arr.stream()
                .map(o -> ((JsonObject) o).mapTo(WithdrawStats.YearStatus.class))
                .collect(Collectors.toList());
            return Future.succeededFuture(data);
          }
          span.setAttribute("cache.hit", false);
          return repository.getYearlyStatusByCard(card, year, status)
              .map(list -> {
                redisService.setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()), CACHE_TTL);
                return list;
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyStatusByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyStatusByCard", e.getMessage()));
  }
}
