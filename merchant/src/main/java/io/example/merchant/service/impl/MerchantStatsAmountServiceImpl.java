package io.example.merchant.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsAmountRepository;
import io.example.merchant.service.MerchantStatsAmountService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.FindYearMerchant;

@RequiredArgsConstructor
public class MerchantStatsAmountServiceImpl implements MerchantStatsAmountService {
  private final MerchantStatsAmountRepository repository;
  private final RedisService redisService;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String CACHE_PREFIX = "stats:amount:global:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  @Override
  public Future<List<MerchantStats.MonthAmount>> getMonthlyAmounts(FindYearMerchant req) {
    var ctx = metrics.startSpan("MerchantStatsAmountService.getMonthlyAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getYear();

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<MerchantStats.MonthAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("merchant_stats.cache_hit", false);
          return repository.getMonthlyAmountMerchant(req.getYear())
              .compose(res -> redisService.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyAmounts", e.getMessage()));
  }

  @Override
  public Future<List<MerchantStats.YearAmount>> getYearlyAmounts(FindYearMerchant req) {
    var ctx = metrics.startSpan("MerchantStatsAmountService.getYearlyAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getYear();

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<MerchantStats.YearAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("merchant_stats.cache_hit", false);
          return repository.getYearlyAmountMerchant(req.getYear())
              .compose(res -> redisService.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyAmounts", e.getMessage()));
  }
}