package io.example.merchant.service.impl;

import java.util.List;
import java.time.Duration;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsAmountRepository;
import io.example.merchant.service.MerchantStatsAmountService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MerchantStatsAmountServiceImpl implements MerchantStatsAmountService {
  private final MerchantStatsAmountRepository repository;
  private final RedisService redisService;
  private final TracingMetrics metrics;
  private static final String CACHE_PREFIX = "stats:amount:global:";

  public MerchantStatsAmountServiceImpl(MerchantStatsAmountRepository repository, RedisService redisService, TracingMetrics metrics) {
    this.repository = repository;
    this.redisService = redisService;
    this.metrics = metrics;
  }

  @Override
  public Future<List<MerchantStats.MonthAmount>> getMonthlyAmounts(pb.merchant.Merchant.FindYearMerchant req) {
    var ctx = metrics.startSpan("MerchantStatsAmountService.getMonthlyAmounts");
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getYear();
    
    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            List<MerchantStats.MonthAmount> list = new JsonArray(cached).stream()
                .map(o -> MerchantStats.MonthAmount.fromJson((JsonObject) o)).toList();
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyAmountMerchant(req.getYear())
              .onSuccess(list -> {
                JsonArray arr = new JsonArray(list.stream().map(MerchantStats.MonthAmount::toJson).toList());
                redisService.setJson(cacheKey, arr, Duration.ofMinutes(10));
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyAmounts", e.getMessage()));
  }

  @Override
  public Future<List<MerchantStats.YearAmount>> getYearlyAmounts(pb.merchant.Merchant.FindYearMerchant req) {
    var ctx = metrics.startSpan("MerchantStatsAmountService.getYearlyAmounts");
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getYear();

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            List<MerchantStats.YearAmount> list = new JsonArray(cached).stream()
                .map(o -> MerchantStats.YearAmount.fromJson((JsonObject) o)).toList();
            return Future.succeededFuture(list);
          }
          return repository.getYearlyAmountMerchant(req.getYear())
              .onSuccess(list -> {
                JsonArray arr = new JsonArray(list.stream().map(MerchantStats.YearAmount::toJson).toList());
                redisService.setJson(cacheKey, arr, Duration.ofMinutes(10));
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyAmounts", e.getMessage()));
  }
}
