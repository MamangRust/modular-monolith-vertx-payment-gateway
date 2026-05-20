package io.example.card.service.impl;

import java.util.List;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsWithdrawByCardRepository;
import io.example.card.repository.CardStatsWithdrawRepository;
import io.example.card.service.CardStatsWithdrawService;
import io.example.common.domain.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import java.time.Duration;

public class CardStatsWithdrawServiceImpl implements CardStatsWithdrawService {
  private final CardStatsWithdrawRepository repository;
  private final CardStatsWithdrawByCardRepository byCardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  public CardStatsWithdrawServiceImpl(CardStatsWithdrawRepository repository, 
                                      CardStatsWithdrawByCardRepository byCardRepository,
                                      RedisService redis,
                                      TracingMetrics metrics) {
    this.repository = repository;
    this.byCardRepository = byCardRepository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyWithdrawAmount(int year) {
    var ctx = metrics.startSpan("CardStatsWithdrawService.getMonthlyWithdrawAmount");
    String cacheKey = "stats:withdraw:monthly:" + year;

    return redis.getJsonList(cacheKey, CardStats.MonthAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return repository.getMonthlyWithdrawAmount(year)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly withdraw amount retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyWithdrawAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyWithdrawAmount", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyWithdrawAmount(int endYear) {
    var ctx = metrics.startSpan("CardStatsWithdrawService.getYearlyWithdrawAmount");
    String cacheKey = "stats:withdraw:yearly:" + endYear;

    return redis.getJsonList(cacheKey, CardStats.YearAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return repository.getYearlyWithdrawAmount(endYear)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly withdraw amount retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyWithdrawAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyWithdrawAmount", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyWithdrawAmountByCardNumber(int year, String cardNum) {
    var ctx = metrics.startSpan("CardStatsWithdrawService.getMonthlyWithdrawAmountByCardNumber");
    String cacheKey = "stats:withdraw:monthly:" + year + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.MonthAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return byCardRepository.getMonthlyWithdrawAmountByCardNumber(year, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly withdraw amount by card retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyWithdrawAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyWithdrawAmountByCardNumber", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyWithdrawAmountByCardNumber(int endYear, String cardNum) {
    var ctx = metrics.startSpan("CardStatsWithdrawService.getYearlyWithdrawAmountByCardNumber");
    String cacheKey = "stats:withdraw:yearly:" + endYear + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.YearAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return byCardRepository.getYearlyWithdrawAmountByCardNumber(endYear, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly withdraw amount by card retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyWithdrawAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyWithdrawAmountByCardNumber", e.getMessage()));
  }
}
