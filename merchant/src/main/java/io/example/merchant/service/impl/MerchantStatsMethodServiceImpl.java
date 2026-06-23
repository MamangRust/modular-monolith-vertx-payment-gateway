package io.example.merchant.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsMethodRepository;
import io.example.merchant.service.MerchantStatsMethodService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.FindYearMerchant;

@RequiredArgsConstructor
public class MerchantStatsMethodServiceImpl implements MerchantStatsMethodService {
  private final MerchantStatsMethodRepository repository;
  private final RedisService redisService;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String CACHE_PREFIX = "stats:method:global:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  @Override
  public Future<List<MerchantStats.MonthMethod>> getMonthlyMethodAmounts(FindYearMerchant req) {
    var ctx = metrics.startSpan("MerchantStatsMethodService.getMonthlyMethodAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getYear();

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<MerchantStats.MonthMethod>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("merchant_stats.cache_hit", false);
          return repository.getMonthlyPaymentMethodsMerchant(req.getYear())
              .compose(res -> redisService.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyMethodAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyMethodAmounts", e.getMessage()));
  }

  @Override
  public Future<List<MerchantStats.YearMethod>> getYearlyMethodAmounts(FindYearMerchant req) {
    var ctx = metrics.startSpan("MerchantStatsMethodService.getYearlyMethodAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getYear();

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<MerchantStats.YearMethod>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("merchant_stats.cache_hit", false);
          return repository.getYearlyPaymentMethodMerchant(req.getYear())
              .compose(res -> redisService.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyMethodAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyMethodAmounts", e.getMessage()));
  }
}