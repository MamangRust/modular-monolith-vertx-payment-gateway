package io.example.card.service.impl;

import java.util.List;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsTransferByCardRepository;
import io.example.card.repository.CardStatsTransferRepository;
import io.example.card.service.CardStatsTransferService;
import io.example.common.domain.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import java.time.Duration;

public class CardStatsTransferServiceImpl implements CardStatsTransferService {
  private final CardStatsTransferRepository repository;
  private final CardStatsTransferByCardRepository byCardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  public CardStatsTransferServiceImpl(CardStatsTransferRepository repository, 
                                      CardStatsTransferByCardRepository byCardRepository,
                                      RedisService redis,
                                      TracingMetrics metrics) {
    this.repository = repository;
    this.byCardRepository = byCardRepository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransferAmountSender(int year) {
    var ctx = metrics.startSpan("CardStatsTransferService.getMonthlyTransferAmountSender");
    String cacheKey = "stats:transfer:monthly:sender:" + year;

    return redis.getJsonList(cacheKey, CardStats.MonthAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return repository.getMonthlyTransferAmountSender(year)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly transfer amount sender retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransferAmountSender", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransferAmountSender", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransferAmountReceiver(int year) {
    var ctx = metrics.startSpan("CardStatsTransferService.getMonthlyTransferAmountReceiver");
    String cacheKey = "stats:transfer:monthly:receiver:" + year;

    return redis.getJsonList(cacheKey, CardStats.MonthAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return repository.getMonthlyTransferAmountReceiver(year)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly transfer amount receiver retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransferAmountReceiver", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransferAmountReceiver", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransferAmountSender(int endYear) {
    var ctx = metrics.startSpan("CardStatsTransferService.getYearlyTransferAmountSender");
    String cacheKey = "stats:transfer:yearly:sender:" + endYear;

    return redis.getJsonList(cacheKey, CardStats.YearAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return repository.getYearlyTransferAmountSender(endYear)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly transfer amount sender retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransferAmountSender", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransferAmountSender", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransferAmountReceiver(int endYear) {
    var ctx = metrics.startSpan("CardStatsTransferService.getYearlyTransferAmountReceiver");
    String cacheKey = "stats:transfer:yearly:receiver:" + endYear;

    return redis.getJsonList(cacheKey, CardStats.YearAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return repository.getYearlyTransferAmountReceiver(endYear)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly transfer amount receiver retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransferAmountReceiver", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransferAmountReceiver", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransferAmountBySender(int year, String cardNum) {
    var ctx = metrics.startSpan("CardStatsTransferService.getMonthlyTransferAmountBySender");
    String cacheKey = "stats:transfer:monthly:sender:" + year + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.MonthAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return byCardRepository.getMonthlyTransferAmountBySender(year, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly transfer amount by sender retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransferAmountBySender", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransferAmountBySender", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.MonthAmount>>> getMonthlyTransferAmountByReceiver(int year, String cardNum) {
    var ctx = metrics.startSpan("CardStatsTransferService.getMonthlyTransferAmountByReceiver");
    String cacheKey = "stats:transfer:monthly:receiver:" + year + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.MonthAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return byCardRepository.getMonthlyTransferAmountByReceiver(year, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Monthly transfer amount by receiver retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransferAmountByReceiver", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransferAmountByReceiver", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransferAmountBySender(int endYear, String cardNum) {
    var ctx = metrics.startSpan("CardStatsTransferService.getYearlyTransferAmountBySender");
    String cacheKey = "stats:transfer:yearly:sender:" + endYear + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.YearAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return byCardRepository.getYearlyTransferAmountBySender(endYear, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly transfer amount by sender retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransferAmountBySender", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransferAmountBySender", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<CardStats.YearAmount>>> getYearlyTransferAmountByReceiver(int endYear, String cardNum) {
    var ctx = metrics.startSpan("CardStatsTransferService.getYearlyTransferAmountByReceiver");
    String cacheKey = "stats:transfer:yearly:receiver:" + endYear + ":" + cardNum;

    return redis.getJsonList(cacheKey, CardStats.YearAmount.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) return Future.succeededFuture(cached);
          return byCardRepository.getYearlyTransferAmountByReceiver(endYear, cardNum)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .map(res -> ApiResponse.success("Yearly transfer amount by receiver retrieved successfully", res))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransferAmountByReceiver", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransferAmountByReceiver", e.getMessage()));
  }
}
