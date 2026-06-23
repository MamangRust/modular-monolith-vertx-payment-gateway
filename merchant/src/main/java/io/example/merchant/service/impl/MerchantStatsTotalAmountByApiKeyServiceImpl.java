package io.example.merchant.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.merchant.MonthYearTotalAmountApiKey;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsTotalAmountByApiKeyRepository;
import io.example.merchant.service.MerchantStatsTotalAmountByApiKeyService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MerchantStatsTotalAmountByApiKeyServiceImpl implements MerchantStatsTotalAmountByApiKeyService {
  private final MerchantStatsTotalAmountByApiKeyRepository repository;
  private final RedisService redisService;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String CACHE_PREFIX = "stats:totalamount:apikey:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  @Override
  public Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmounts(MonthYearTotalAmountApiKey req) {
    var ctx = metrics.startSpan("MerchantStatsTotalAmountByApiKeyService.getMonthlyTotalAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getApikey() + ":" + req.getYear();

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
          return repository.getMonthlyTotalAmountByApikey(req)
              .compose(res -> redisService.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTotalAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTotalAmounts", e.getMessage()));
  }

  @Override
  public Future<List<MerchantStats.YearAmount>> getYearlyTotalAmounts(MonthYearTotalAmountApiKey req) {
    var ctx = metrics.startSpan("MerchantStatsTotalAmountByApiKeyService.getYearlyTotalAmounts");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getApikey() + ":" + req.getYear();

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
          return repository.getYearlyTotalAmountByApikey(req)
              .compose(res -> redisService.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTotalAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTotalAmounts", e.getMessage()));
  }
}