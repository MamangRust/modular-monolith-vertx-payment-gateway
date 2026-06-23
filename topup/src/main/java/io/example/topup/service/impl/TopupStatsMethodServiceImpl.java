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
import io.example.topup.repository.TopupStatsByCardMethodRepository;
import io.example.topup.repository.TopupStatsMethodRepository;
import io.example.topup.service.TopupStatsMethodService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TopupStatsMethodServiceImpl implements TopupStatsMethodService {
  private final TopupStatsMethodRepository repository;
  private final TopupStatsByCardMethodRepository cardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String CACHE_PREFIX = "topup:stats:method:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  @Override
  public Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethods(YearTopupRequest req) {
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsMethodService.getMonthlyTopupMethods");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.MonthMethod>>() {
              });
              return Future.<List<TopupStats.MonthMethod>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);
          Future<List<TopupStats.MonthMethod>> fromDb = repository.getMonthlyTopupMethods(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupMethods", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupMethods", e.getMessage()));
  }

  @Override
  public Future<List<TopupStats.YearMethod>> getYearlyTopupMethods(YearTopupRequest req) {
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsMethodService.getYearlyTopupMethods");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.YearMethod>>() {
              });
              return Future.<List<TopupStats.YearMethod>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);
          Future<List<TopupStats.YearMethod>> fromDb = repository.getYearlyTopupMethods(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupMethods", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupMethods", e.getMessage()));
  }

  @Override
  public Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethodsByCard(YearTopupCardNumberRequest req) {
    String cacheKey = CACHE_PREFIX + "monthly:card:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsMethodService.getMonthlyTopupMethodsByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.MonthMethod>>() {
              });
              return Future.<List<TopupStats.MonthMethod>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);
          Future<List<TopupStats.MonthMethod>> fromDb = cardRepository.getMonthlyTopupMethodsByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupMethodsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupMethodsByCard", e.getMessage()));
  }

  @Override
  public Future<List<TopupStats.YearMethod>> getYearlyTopupMethodsByCard(YearTopupCardNumberRequest req) {
    String cacheKey = CACHE_PREFIX + "yearly:card:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsMethodService.getYearlyTopupMethodsByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.YearMethod>>() {
              });
              return Future.<List<TopupStats.YearMethod>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);
          Future<List<TopupStats.YearMethod>> fromDb = cardRepository.getYearlyTopupMethodsByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupMethodsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupMethodsByCard", e.getMessage()));
  }
}