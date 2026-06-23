package io.example.topup.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.topup.domain.requests.topup.MonthTopupStatusCardNumberRequest;
import io.example.topup.domain.requests.topup.MonthTopupStatusRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusRequest;
import io.example.topup.model.TopupStats;
import io.example.topup.repository.TopupStatsByCardStatusRepository;
import io.example.topup.repository.TopupStatsStatusRepository;
import io.example.topup.service.TopupStatsStatusService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TopupStatsStatusServiceImpl implements TopupStatsStatusService {
  private final TopupStatsStatusRepository repository;
  private final TopupStatsByCardStatusRepository cardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String CACHE_PREFIX = "topup:stats:status:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  @Override
  public Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatus(MonthTopupStatusRequest req) {
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getStatus() + ":" + req.getYear() + ":" + req.getMonth();
    var ctx = metrics.startSpan("TopupStatsStatusService.getMonthlyTopupStatus");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.MonthStatus>>() {
              });
              return Future.<List<TopupStats.MonthStatus>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);
          Future<List<TopupStats.MonthStatus>> fromDb = repository.getMonthlyTopupStatus(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupStatus", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupStatus", e.getMessage()));
  }

  @Override
  public Future<List<TopupStats.YearStatus>> getYearlyTopupStatus(YearTopupStatusRequest req) {
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getStatus() + ":" + req.getYear();
    var ctx = metrics.startSpan("TopupStatsStatusService.getYearlyTopupStatus");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.YearStatus>>() {
              });
              return Future.<List<TopupStats.YearStatus>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);
          Future<List<TopupStats.YearStatus>> fromDb = repository.getYearlyTopupStatus(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupStatus", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupStatus", e.getMessage()));
  }

  @Override
  public Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatusByCard(MonthTopupStatusCardNumberRequest req) {
    String cacheKey = CACHE_PREFIX + "monthly:card:" + req.getStatus() + ":" + req.getCardNumber() + ":"
        + req.getYear() + ":" + req.getMonth();
    var ctx = metrics.startSpan("TopupStatsStatusService.getMonthlyTopupStatusByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.MonthStatus>>() {
              });
              return Future.<List<TopupStats.MonthStatus>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);
          Future<List<TopupStats.MonthStatus>> fromDb = cardRepository.getMonthlyTopupStatusByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTopupStatusByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTopupStatusByCard", e.getMessage()));
  }

  @Override
  public Future<List<TopupStats.YearStatus>> getYearlyTopupStatusByCard(YearTopupStatusCardNumberRequest req) {
    String cacheKey = CACHE_PREFIX + "yearly:card:" + req.getStatus() + ":" + req.getCardNumber() + ":"
        + req.getYear();
    var ctx = metrics.startSpan("TopupStatsStatusService.getYearlyTopupStatusByCard");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<TopupStats.YearStatus>>() {
              });
              return Future.<List<TopupStats.YearStatus>>succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("cache.hit", false);
          Future<List<TopupStats.YearStatus>> fromDb = cardRepository.getYearlyTopupStatusByCard(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
          return fromDb;
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTopupStatusByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTopupStatusByCard", e.getMessage()));
  }
}