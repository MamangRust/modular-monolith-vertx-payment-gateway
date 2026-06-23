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
import io.example.transaction.repository.TransactionStatsAmountRepository;
import io.example.transaction.service.TransactionStatsAmountService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransactionStatsAmountServiceImpl implements TransactionStatsAmountService {
  private final TransactionStatsAmountRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String CACHE_PREFIX = "transaction:stats:amount:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  @Override
  public Future<List<TransactionStats.MonthAmount>> getMonthlyAmounts(YearTransactionRequest req) {
    var ctx = metrics.startSpan("TransactionStatsAmountService.getMonthlyAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getYear();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.MonthAmount>>() {
              });
              return Future.<List<TransactionStats.MonthAmount>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback to DB */
            }
          }

          span.setAttribute("cache.hit", false);

          Future<List<TransactionStats.MonthAmount>> fromDb = repository.getMonthlyAmounts(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));

          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyAmounts", e.getMessage()));
  }

  @Override
  public Future<List<TransactionStats.YearAmount>> getYearlyAmounts(YearTransactionRequest req) {
    var ctx = metrics.startSpan("TransactionStatsAmountService.getYearlyAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getYear();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.YearAmount>>() {
              });
              return Future.<List<TransactionStats.YearAmount>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback to DB */ }
          }
          span.setAttribute("cache.hit", false);
          return repository.getYearlyAmounts(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyAmounts", e.getMessage()));
  }

  @Override
  public Future<List<TransactionStats.MonthAmount>> getMonthlyAmountsByCard(YearCardNumberTransactionRequest req) {
    var ctx = metrics.startSpan("TransactionStatsAmountService.getMonthlyAmountsByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "monthly:card:" + req.getCardNumber() + ":" + req.getYear();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.MonthAmount>>() {
              });
              return Future.<List<TransactionStats.MonthAmount>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback to DB */ }
          }
          span.setAttribute("cache.hit", false);
          return repository.getMonthlyAmountsByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyAmountsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyAmountsByCard", e.getMessage()));
  }

  @Override
  public Future<List<TransactionStats.YearAmount>> getYearlyAmountsByCard(YearCardNumberTransactionRequest req) {
    var ctx = metrics.startSpan("TransactionStatsAmountService.getYearlyAmountsByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "yearly:card:" + req.getCardNumber() + ":" + req.getYear();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TransactionStats.YearAmount>>() {
              });
              return Future.<List<TransactionStats.YearAmount>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback to DB */ }
          }
          span.setAttribute("cache.hit", false);
          return repository.getYearlyAmountsByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyAmountsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyAmountsByCard", e.getMessage()));
  }
}