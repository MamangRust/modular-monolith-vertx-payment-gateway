package io.example.withdraw.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.withdraw.domain.requests.YearMonthCardNumber;
import io.example.withdraw.model.WithdrawStats;
import io.example.withdraw.repository.WithdrawStatsAmountRepository;
import io.example.withdraw.service.WithdrawStatsAmountService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WithdrawStatsAmountServiceImpl implements WithdrawStatsAmountService {
  private final WithdrawStatsAmountRepository repository;
  private final RedisService redisService;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "withdraw:stats:amount:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  @Override
  public Future<List<WithdrawStats.MonthAmount>> getMonthlyWithdrawAmounts(int year) {
    var ctx = metrics.startSpan("WithdrawStatsAmountService.getMonthlyWithdrawAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "monthly:" + year;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            JsonArray arr = new JsonArray(cached);
            List<WithdrawStats.MonthAmount> data = arr.stream()
                .map(o -> ((JsonObject) o).mapTo(WithdrawStats.MonthAmount.class))
                .toList();
            return Future.succeededFuture(data);
          }
          span.setAttribute("cache.hit", false);
          return repository.getMonthlyWithdrawAmounts(year)
              .compose(list -> redisService
                  .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()), CACHE_TTL)
                  .map(v -> list));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyWithdrawAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyWithdrawAmounts", e.getMessage()));
  }

  @Override
  public Future<List<WithdrawStats.YearAmount>> getYearlyWithdrawAmounts(int year) {
    var ctx = metrics.startSpan("WithdrawStatsAmountService.getYearlyWithdrawAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "yearly:" + year;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            JsonArray arr = new JsonArray(cached);
            List<WithdrawStats.YearAmount> data = arr.stream()
                .map(o -> ((JsonObject) o).mapTo(WithdrawStats.YearAmount.class))
                .toList();
            return Future.succeededFuture(data);
          }
          span.setAttribute("cache.hit", false);
          return repository.getYearlyWithdrawAmounts(year)
              .compose(list -> redisService
                  .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()), CACHE_TTL)
                  .map(v -> list));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyWithdrawAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyWithdrawAmounts", e.getMessage()));
  }

  @Override
  public Future<List<WithdrawStats.MonthAmount>> getMonthlyWithdrawAmountsByCard(YearMonthCardNumber req) {
    var ctx = metrics.startSpan("WithdrawStatsAmountService.getMonthlyWithdrawAmountsByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "card:monthly:" + req.getCardNumber() + ":" + req.getYear();

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            JsonArray arr = new JsonArray(cached);
            List<WithdrawStats.MonthAmount> data = arr.stream()
                .map(o -> ((JsonObject) o).mapTo(WithdrawStats.MonthAmount.class))
                .toList();
            return Future.succeededFuture(data);
          }
          span.setAttribute("cache.hit", false);
          return repository.getMonthlyWithdrawAmountsByCard(req)
              .compose(list -> redisService
                  .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()), CACHE_TTL)
                  .map(v -> list));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyWithdrawAmountsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyWithdrawAmountsByCard", e.getMessage()));
  }

  @Override
  public Future<List<WithdrawStats.YearAmount>> getYearlyWithdrawAmountsByCard(YearMonthCardNumber req) {
    var ctx = metrics.startSpan("WithdrawStatsAmountService.getYearlyWithdrawAmountsByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "card:yearly:" + req.getCardNumber() + ":" + req.getYear();

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            JsonArray arr = new JsonArray(cached);
            List<WithdrawStats.YearAmount> data = arr.stream()
                .map(o -> ((JsonObject) o).mapTo(WithdrawStats.YearAmount.class))
                .toList();
            return Future.succeededFuture(data);
          }
          span.setAttribute("cache.hit", false);
          return repository.getYearlyWithdrawAmountsByCard(req)
              .compose(list -> redisService
                  .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()), CACHE_TTL)
                  .map(v -> list));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyWithdrawAmountsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyWithdrawAmountsByCard", e.getMessage()));
  }
}