package io.example.card.service.impl;

import java.util.List;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsTopupByCardRepository;
import io.example.card.repository.CardStatsTopupRepository;
import io.example.card.service.CardStatsTopupService;
import io.example.common.domain.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import java.time.Duration;

public class CardStatsTopupServiceImpl implements CardStatsTopupService {
  private final CardStatsTopupRepository repository;
  private final CardStatsTopupByCardRepository byCardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  public CardStatsTopupServiceImpl(CardStatsTopupRepository repository, 
                                   CardStatsTopupByCardRepository byCardRepository,
                                   RedisService redis,
                                   TracingMetrics metrics) {
    this.repository = repository;
    this.byCardRepository = byCardRepository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTopupAmount(int year) {
    var ctx = metrics.startSpan("CardStatsTopupService.getMonthlyTopupAmount");
    String cacheKey = "stats:topup:monthly:" + year;

    return redis.getJsonList(cacheKey, CardStats.MonthAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return repository.getMonthlyTopupAmount(year)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly topup amount retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupAmount", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTopupAmount(int endYear) {
    var ctx = metrics.startSpan("CardStatsTopupService.getYearlyTopupAmount");
    String cacheKey = "stats:topup:yearly:" + endYear;

    return redis.getJsonList(cacheKey, CardStats.YearAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return repository.getYearlyTopupAmount(endYear)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly topup amount retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupAmount", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTopupAmountByCardNumber(int year, String cardNum) {
    var ctx = metrics.startSpan("CardStatsTopupService.getMonthlyTopupAmountByCardNumber");
    String cacheKey = "stats:topup:monthly:" + year + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.MonthAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return byCardRepository.getMonthlyTopupAmountByCardNumber(year, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly topup amount by card retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupAmountByCardNumber", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTopupAmountByCardNumber(int endYear, String cardNum) {
    var ctx = metrics.startSpan("CardStatsTopupService.getYearlyTopupAmountByCardNumber");
    String cacheKey = "stats:topup:yearly:" + endYear + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.YearAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return byCardRepository.getYearlyTopupAmountByCardNumber(endYear, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly topup amount by card retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupAmountByCardNumber", e.getMessage()));
  }
}
