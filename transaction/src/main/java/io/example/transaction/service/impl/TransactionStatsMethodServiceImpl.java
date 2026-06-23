package io.example.transaction.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.domain.requests.YearCardNumberTransactionRequest;
import io.example.transaction.domain.requests.YearTransactionRequest;
import io.example.transaction.model.TransactionStats;
import io.example.transaction.repository.TransactionStatsMethodRepository;
import io.example.transaction.service.TransactionStatsMethodService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransactionStatsMethodServiceImpl implements TransactionStatsMethodService {
  private final TransactionStatsMethodRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String CACHE_PREFIX = "transaction:stats:method:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  @Override
  public Future<List<TransactionStats.MonthMethod>> getMonthlyMethods(YearTransactionRequest req) {
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getYear();
    var ctx = metrics.startSpan("TransactionStatsMethodService.getMonthlyMethods");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.MonthMethod>>() {
              });
              return Future.<List<TransactionStats.MonthMethod>>succeededFuture(list);
            } catch (Exception e) {
            }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TransactionStats.MonthMethod>> fromDb = repository.getMonthlyMethods(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyMethods", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyMethods", e.getMessage()));
  }

  @Override
  public Future<List<TransactionStats.YearMethod>> getYearlyMethods(YearTransactionRequest req) {
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getYear();
    var ctx = metrics.startSpan("TransactionStatsMethodService.getYearlyMethods");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.YearMethod>>() {
              });
              return Future.<List<TransactionStats.YearMethod>>succeededFuture(list);
            } catch (Exception e) {
            }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TransactionStats.YearMethod>> fromDb = repository.getYearlyMethods(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyMethods", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyMethods", e.getMessage()));
  }

  @Override
  public Future<List<TransactionStats.MonthMethod>> getMonthlyMethodsByCard(YearCardNumberTransactionRequest req) {
    String cacheKey = CACHE_PREFIX + "monthly:card:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TransactionStatsMethodService.getMonthlyMethodsByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.MonthMethod>>() {
              });
              return Future.<List<TransactionStats.MonthMethod>>succeededFuture(list);
            } catch (Exception e) {
            }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TransactionStats.MonthMethod>> fromDb = repository.getMonthlyMethodsByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyMethodsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyMethodsByCard", e.getMessage()));
  }

  @Override
  public Future<List<TransactionStats.YearMethod>> getYearlyMethodsByCard(YearCardNumberTransactionRequest req) {
    String cacheKey = CACHE_PREFIX + "yearly:card:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TransactionStatsMethodService.getYearlyMethodsByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.YearMethod>>() {
              });
              return Future.<List<TransactionStats.YearMethod>>succeededFuture(list);
            } catch (Exception e) {
            }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TransactionStats.YearMethod>> fromDb = repository.getYearlyMethodsByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyMethodsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyMethodsByCard", e.getMessage()));
  }
}