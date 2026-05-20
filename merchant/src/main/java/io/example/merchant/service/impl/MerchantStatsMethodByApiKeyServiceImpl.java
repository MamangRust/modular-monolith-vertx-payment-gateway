package io.example.merchant.service.impl;

import java.util.List;
import java.time.Duration;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsMethodByApiKeyRepository;
import io.example.merchant.service.MerchantStatsMethodByApiKeyService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MerchantStatsMethodByApiKeyServiceImpl implements MerchantStatsMethodByApiKeyService {
  private final MerchantStatsMethodByApiKeyRepository repository;
  private final RedisService redisService;
  private final TracingMetrics metrics;
  private static final String CACHE_PREFIX = "stats:method:apikey:";

  public MerchantStatsMethodByApiKeyServiceImpl(MerchantStatsMethodByApiKeyRepository repository, RedisService redisService, TracingMetrics metrics) {
    this.repository = repository;
    this.redisService = redisService;
    this.metrics = metrics;
  }

  @Override
  public Future<List<MerchantStats.MonthMethod>> getMonthlyMethodAmounts(pb.merchant.Merchant.FindYearMerchantByApikey req) {
    var ctx = metrics.startSpan("MerchantStatsMethodByApiKeyService.getMonthlyMethodAmounts");
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getApiKey() + ":" + req.getYear();

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            List<MerchantStats.MonthMethod> list = new JsonArray(cached).stream()
                .map(o -> MerchantStats.MonthMethod.fromJson((JsonObject) o)).toList();
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyPaymentMethodByApikey(req)
              .onSuccess(list -> {
                JsonArray arr = new JsonArray(list.stream().map(MerchantStats.MonthMethod::toJson).toList());
                redisService.setJson(cacheKey, arr, Duration.ofMinutes(10));
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyMethodAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyMethodAmounts", e.getMessage()));
  }

  @Override
  public Future<List<MerchantStats.YearMethod>> getYearlyMethodAmounts(pb.merchant.Merchant.FindYearMerchantByApikey req) {
    var ctx = metrics.startSpan("MerchantStatsMethodByApiKeyService.getYearlyMethodAmounts");
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getApiKey() + ":" + req.getYear();

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            List<MerchantStats.YearMethod> list = new JsonArray(cached).stream()
                .map(o -> MerchantStats.YearMethod.fromJson((JsonObject) o)).toList();
            return Future.succeededFuture(list);
          }
          return repository.getYearlyPaymentMethodByApikey(req)
              .onSuccess(list -> {
                JsonArray arr = new JsonArray(list.stream().map(MerchantStats.YearMethod::toJson).toList());
                redisService.setJson(cacheKey, arr, Duration.ofMinutes(10));
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyMethodAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyMethodAmounts", e.getMessage()));
  }
}
