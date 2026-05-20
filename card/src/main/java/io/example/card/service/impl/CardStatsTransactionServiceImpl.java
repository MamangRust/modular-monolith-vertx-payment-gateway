package io.example.card.service.impl;

import java.util.List;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsTransactionByCardRepository;
import io.example.card.repository.CardStatsTransactionRepository;
import io.example.card.service.CardStatsTransactionService;
import io.example.common.domain.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import java.time.Duration;

public class CardStatsTransactionServiceImpl implements CardStatsTransactionService {
  private final CardStatsTransactionRepository repository;
  private final CardStatsTransactionByCardRepository byCardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  public CardStatsTransactionServiceImpl(CardStatsTransactionRepository repository, 
                                         CardStatsTransactionByCardRepository byCardRepository,
                                         RedisService redis,
                                         TracingMetrics metrics) {
    this.repository = repository;
    this.byCardRepository = byCardRepository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransactionAmount(int year) {
    var ctx = metrics.startSpan("CardStatsTransactionService.getMonthlyTransactionAmount");
    String cacheKey = "stats:transaction:monthly:" + year;

    return redis.getJsonList(cacheKey, CardStats.MonthAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return repository.getMonthlyTransactionAmount(year)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly transaction amount retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransactionAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransactionAmount", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransactionAmount(int endYear) {
    var ctx = metrics.startSpan("CardStatsTransactionService.getYearlyTransactionAmount");
    String cacheKey = "stats:transaction:yearly:" + endYear;

    return redis.getJsonList(cacheKey, CardStats.YearAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return repository.getYearlyTransactionAmount(endYear)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly transaction amount retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransactionAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransactionAmount", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransactionAmountByCardNumber(int year, String cardNum) {
    var ctx = metrics.startSpan("CardStatsTransactionService.getMonthlyTransactionAmountByCardNumber");
    String cacheKey = "stats:transaction:monthly:" + year + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.MonthAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return byCardRepository.getMonthlyTransactionAmountByCardNumber(year, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly transaction amount by card retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransactionAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransactionAmountByCardNumber", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransactionAmountByCardNumber(int endYear, String cardNum) {
    var ctx = metrics.startSpan("CardStatsTransactionService.getYearlyTransactionAmountByCardNumber");
    String cacheKey = "stats:transaction:yearly:" + endYear + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.YearAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return byCardRepository.getYearlyTransactionAmountByCardNumber(endYear, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly transaction amount by card retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransactionAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransactionAmountByCardNumber", e.getMessage()));
  }
}
