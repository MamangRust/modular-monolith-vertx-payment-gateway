package io.example.card.service.impl;

import java.util.List;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsBalanceByCardRepository;
import io.example.card.repository.CardStatsBalanceRepository;
import io.example.card.service.CardStatsBalanceService;
import io.example.common.domain.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import java.time.Duration;

public class CardStatsBalanceServiceImpl implements CardStatsBalanceService {
  private final CardStatsBalanceRepository repository;
  private final CardStatsBalanceByCardRepository byCardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  public CardStatsBalanceServiceImpl(CardStatsBalanceRepository repository, 
                                     CardStatsBalanceByCardRepository byCardRepository,
                                     RedisService redis,
                                     TracingMetrics metrics) {
    this.repository = repository;
    this.byCardRepository = byCardRepository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthBalance>>> getMonthlyBalances(int year) {
    var ctx = metrics.startSpan("CardStatsBalanceService.getMonthlyBalances");
    String cacheKey = "stats:balance:monthly:" + year;

    return redis.getJsonList(cacheKey, CardStats.MonthBalance.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached);
          }
          return repository.getMonthlyBalances(year)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly balances retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyBalances", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyBalances", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearlyBalance>>> getYearlyBalances(int endYear) {
    var ctx = metrics.startSpan("CardStatsBalanceService.getYearlyBalances");
    String cacheKey = "stats:balance:yearly:" + endYear;

    return redis.getJsonList(cacheKey, CardStats.YearlyBalance.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached);
          }
          return repository.getYearlyBalances(endYear)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly balances retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyBalances", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyBalances", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthBalance>>> getMonthlyBalancesByCardNumber(int year, String cardNum) {
    var ctx = metrics.startSpan("CardStatsBalanceService.getMonthlyBalancesByCardNumber");
    String cacheKey = "stats:balance:monthly:" + year + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.MonthBalance.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached);
          }
          return byCardRepository.getMonthlyBalancesByCardNumber(year, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly balances by card retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyBalancesByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyBalancesByCardNumber", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearlyBalance>>> getYearlyBalancesByCardNumber(int endYear, String cardNum) {
    var ctx = metrics.startSpan("CardStatsBalanceService.getYearlyBalancesByCardNumber");
    String cacheKey = "stats:balance:yearly:" + endYear + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.YearlyBalance.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached);
          }
          return byCardRepository.getYearlyBalancesByCardNumber(endYear, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly balances by card retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyBalancesByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyBalancesByCardNumber", e.getMessage()));
  }
}
