package io.example.card.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsTransactionByCardRepository;
import io.example.card.repository.CardStatsTransactionRepository;
import io.example.card.service.CardStatsTransactionService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CardStatsTransactionServiceImpl implements CardStatsTransactionService {
  private final CardStatsTransactionRepository repository;
  private final CardStatsTransactionByCardRepository byCardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  @Override
  public Future<List<CardStats.MonthAmount>> getMonthlyTransactionAmount(int year) {
    var ctx = metrics.startSpan("CardStatsTransactionService.getMonthlyTransactionAmount");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transaction:monthly:" + year;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.MonthAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return repository.getMonthlyTransactionAmount(year)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransactionAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransactionAmount", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearAmount>> getYearlyTransactionAmount(int endYear) {
    var ctx = metrics.startSpan("CardStatsTransactionService.getYearlyTransactionAmount");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transaction:yearly:" + endYear;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.YearAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return repository.getYearlyTransactionAmount(endYear)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransactionAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransactionAmount", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.MonthAmount>> getMonthlyTransactionAmountByCardNumber(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsTransactionService.getMonthlyTransactionAmountByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transaction:monthly:" + req.getYear() + ":" + req.getCardNumber();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.MonthAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return byCardRepository.getMonthlyTransactionAmountByCardNumber(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransactionAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransactionAmountByCardNumber", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearAmount>> getYearlyTransactionAmountByCardNumber(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsTransactionService.getYearlyTransactionAmountByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transaction:yearly:" + req.getYear() + ":" + req.getCardNumber();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.YearAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return byCardRepository.getYearlyTransactionAmountByCardNumber(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransactionAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransactionAmountByCardNumber", e.getMessage()));
  }
}