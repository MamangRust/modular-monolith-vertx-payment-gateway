package io.example.card.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsWithdrawByCardRepository;
import io.example.card.repository.CardStatsWithdrawRepository;
import io.example.card.service.CardStatsWithdrawService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CardStatsWithdrawServiceImpl implements CardStatsWithdrawService {
  private final CardStatsWithdrawRepository repository;
  private final CardStatsWithdrawByCardRepository byCardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  @Override
  public Future<List<CardStats.MonthAmount>> getMonthlyWithdrawAmount(int year) {
    var ctx = metrics.startSpan("CardStatsWithdrawService.getMonthlyWithdrawAmount");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:withdraw:monthly:" + year;

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
          return repository.getMonthlyWithdrawAmount(year)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyWithdrawAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyWithdrawAmount", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearAmount>> getYearlyWithdrawAmount(int endYear) {
    var ctx = metrics.startSpan("CardStatsWithdrawService.getYearlyWithdrawAmount");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:withdraw:yearly:" + endYear;

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
          return repository.getYearlyWithdrawAmount(endYear)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyWithdrawAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyWithdrawAmount", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.MonthAmount>> getMonthlyWithdrawAmountByCardNumber(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsWithdrawService.getMonthlyWithdrawAmountByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:withdraw:monthly:" + req.getYear() + ":" + req.getCardNumber();

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
          return byCardRepository.getMonthlyWithdrawAmountByCardNumber(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyWithdrawAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyWithdrawAmountByCardNumber", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearAmount>> getYearlyWithdrawAmountByCardNumber(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsWithdrawService.getYearlyWithdrawAmountByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:withdraw:yearly:" + req.getYear() + ":" + req.getCardNumber();

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
          return byCardRepository.getYearlyWithdrawAmountByCardNumber(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyWithdrawAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyWithdrawAmountByCardNumber", e.getMessage()));
  }
}