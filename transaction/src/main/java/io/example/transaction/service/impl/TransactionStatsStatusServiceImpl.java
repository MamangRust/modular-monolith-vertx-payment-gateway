package io.example.transaction.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.domain.requests.MonthStatusTransaction;
import io.example.transaction.domain.requests.MonthStatusTransactionCardNumber;
import io.example.transaction.domain.requests.YearStatusTransaction;
import io.example.transaction.domain.requests.YearStatusTransactionCardNumber;
import io.example.transaction.model.TransactionStats;
import io.example.transaction.repository.TransactionStatsStatusRepository;
import io.example.transaction.service.TransactionStatsStatusService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransactionStatsStatusServiceImpl implements TransactionStatsStatusService {
  private final TransactionStatsStatusRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String CACHE_PREFIX = "transaction:stats:status:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  @Override
  public Future<List<TransactionStats.MonthStatus>> getMonthlyStatus(MonthStatusTransaction req) {
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getStatus() + ":" + req.getYear() + ":" + req.getMonth();
    var ctx = metrics.startSpan("TransactionStatsStatusService.getMonthlyStatus");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.MonthStatus>>() {
              });
              return Future.<List<TransactionStats.MonthStatus>>succeededFuture(list);
            } catch (Exception e) {
            }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TransactionStats.MonthStatus>> fromDb = repository.getMonthlyStatus(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyStatus", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyStatus", e.getMessage()));
  }

  @Override
  public Future<List<TransactionStats.YearStatus>> getYearlyStatus(YearStatusTransaction req) {
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getStatus() + ":" + req.getYear();
    var ctx = metrics.startSpan("TransactionStatsStatusService.getYearlyStatus");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.YearStatus>>() {
              });
              return Future.<List<TransactionStats.YearStatus>>succeededFuture(list);
            } catch (Exception e) {
            }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TransactionStats.YearStatus>> fromDb = repository.getYearlyStatus(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyStatus", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyStatus", e.getMessage()));
  }

  @Override
  public Future<List<TransactionStats.MonthStatus>> getMonthlyStatusByCard(MonthStatusTransactionCardNumber req) {
    String cacheKey = CACHE_PREFIX + "monthly:card:" + req.getStatus() + ":" + req.getCardNumber() + ":" + req.getYear()
        + ":" + req.getMonth();
    var ctx = metrics.startSpan("TransactionStatsStatusService.getMonthlyStatusByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.MonthStatus>>() {
              });
              return Future.<List<TransactionStats.MonthStatus>>succeededFuture(list);
            } catch (Exception e) {
            }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TransactionStats.MonthStatus>> fromDb = repository.getMonthlyStatusByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyStatusByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyStatusByCard", e.getMessage()));
  }

  @Override
  public Future<List<TransactionStats.YearStatus>> getYearlyStatusByCard(YearStatusTransactionCardNumber req) {
    String cacheKey = CACHE_PREFIX + "yearly:card:" + req.getStatus() + ":" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TransactionStatsStatusService.getYearlyStatusByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.YearStatus>>() {
              });
              return Future.<List<TransactionStats.YearStatus>>succeededFuture(list);
            } catch (Exception e) {
            }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TransactionStats.YearStatus>> fromDb = repository.getYearlyStatusByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyStatusByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyStatusByCard", e.getMessage()));
  }
}