package io.example.card.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsTopupByCardRepository;
import io.example.card.repository.CardStatsTopupRepository;
import io.example.card.service.CardStatsTopupService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CardStatsTopupServiceImpl implements CardStatsTopupService {
  private final CardStatsTopupRepository repository;
  private final CardStatsTopupByCardRepository byCardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  @Override
  public Future<List<CardStats.MonthAmount>> getMonthlyTopupAmount(int year) {
    var ctx = metrics.startSpan("CardStatsTopupService.getMonthlyTopupAmount");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:topup:monthly:" + year;

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
          return repository.getMonthlyTopupAmount(year)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupAmount", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearAmount>> getYearlyTopupAmount(int endYear) {
    var ctx = metrics.startSpan("CardStatsTopupService.getYearlyTopupAmount");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:topup:yearly:" + endYear;

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
          return repository.getYearlyTopupAmount(endYear)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupAmount", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupAmount", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.MonthAmount>> getMonthlyTopupAmountByCardNumber(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsTopupService.getMonthlyTopupAmountByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:topup:monthly:" + req.getYear() + ":" + req.getCardNumber();

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
          return byCardRepository.getMonthlyTopupAmountByCardNumber(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupAmountByCardNumber", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearAmount>> getYearlyTopupAmountByCardNumber(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsTopupService.getYearlyTopupAmountByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:topup:yearly:" + req.getYear() + ":" + req.getCardNumber();

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
          return byCardRepository.getYearlyTopupAmountByCardNumber(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupAmountByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupAmountByCardNumber", e.getMessage()));
  }
}