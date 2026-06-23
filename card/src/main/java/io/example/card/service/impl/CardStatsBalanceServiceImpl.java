package io.example.card.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsBalanceByCardRepository;
import io.example.card.repository.CardStatsBalanceRepository;
import io.example.card.service.CardStatsBalanceService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CardStatsBalanceServiceImpl implements CardStatsBalanceService {
  private final CardStatsBalanceRepository repository;
  private final CardStatsBalanceByCardRepository byCardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  @Override
  public Future<List<CardStats.MonthBalance>> getMonthlyBalances(int year) {
    var ctx = metrics.startSpan("CardStatsBalanceService.getMonthlyBalances");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:balance:monthly:" + year;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.MonthBalance>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return repository.getMonthlyBalances(year)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyBalances", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyBalances", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearlyBalance>> getYearlyBalances(int endYear) {
    var ctx = metrics.startSpan("CardStatsBalanceService.getYearlyBalances");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:balance:yearly:" + endYear;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.YearlyBalance>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return repository.getYearlyBalances(endYear)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyBalances", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyBalances", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.MonthBalance>> getMonthlyBalancesByCardNumber(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsBalanceService.getMonthlyBalancesByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:balance:monthly:" + req.getYear() + ":" + req.getCardNumber();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.MonthBalance>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return byCardRepository.getMonthlyBalancesByCardNumber(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyBalancesByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyBalancesByCardNumber", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearlyBalance>> getYearlyBalancesByCardNumber(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsBalanceService.getYearlyBalancesByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:balance:yearly:" + req.getYear() + ":" + req.getCardNumber();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.YearlyBalance>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return byCardRepository.getYearlyBalancesByCardNumber(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyBalancesByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyBalancesByCardNumber", e.getMessage()));
  }
}