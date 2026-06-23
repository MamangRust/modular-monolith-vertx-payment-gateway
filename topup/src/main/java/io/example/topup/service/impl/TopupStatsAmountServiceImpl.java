package io.example.topup.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.topup.domain.requests.topup.YearTopupCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupRequest;
import io.example.topup.model.TopupStats;
import io.example.topup.repository.TopupStatsAmountRepository;
import io.example.topup.repository.TopupStatsByCardAmountRepository;
import io.example.topup.service.TopupStatsAmountService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TopupStatsAmountServiceImpl implements TopupStatsAmountService {
  private final TopupStatsAmountRepository repository;
  private final TopupStatsByCardAmountRepository cardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String CACHE_PREFIX = "topup:stats:amount:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  @Override
  public Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmounts(YearTopupRequest req) {
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsAmountService.getMonthlyTopupAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.MonthAmount>>() {
              });
              return Future.<List<TopupStats.MonthAmount>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TopupStats.MonthAmount>> fromDb = repository.getMonthlyTopupAmounts(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupAmounts", e.getMessage()));
  }

  @Override
  public Future<List<TopupStats.YearAmount>> getYearlyTopupAmounts(YearTopupRequest req) {
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsAmountService.getYearlyTopupAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.YearAmount>>() {
              });
              return Future.<List<TopupStats.YearAmount>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TopupStats.YearAmount>> fromDb = repository.getYearlyTopupAmounts(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupAmounts", e.getMessage()));
  }

  @Override
  public Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmountsByCard(YearTopupCardNumberRequest req) {
    String cacheKey = CACHE_PREFIX + "monthly:card:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsAmountService.getMonthlyTopupAmountsByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.MonthAmount>>() {
              });
              return Future.<List<TopupStats.MonthAmount>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TopupStats.MonthAmount>> fromDb = cardRepository.getMonthlyTopupAmountsByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupAmountsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupAmountsByCard", e.getMessage()));
  }

  @Override
  public Future<List<TopupStats.YearAmount>> getYearlyTopupAmountsByCard(YearTopupCardNumberRequest req) {
    String cacheKey = CACHE_PREFIX + "yearly:card:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsAmountService.getYearlyTopupAmountsByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.YearAmount>>() {
              });
              return Future.<List<TopupStats.YearAmount>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);

          Future<List<TopupStats.YearAmount>> fromDb = cardRepository.getYearlyTopupAmountsByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupAmountsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupAmountsByCard", e.getMessage()));
  }
}