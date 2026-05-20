package io.example.merchant.service.impl;

import java.util.List;
import java.time.Duration;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsTotalAmountByMerchantRepository;
import io.example.merchant.service.MerchantStatsTotalAmountByMerchantService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MerchantStatsTotalAmountByMerchantServiceImpl implements MerchantStatsTotalAmountByMerchantService {
  private final MerchantStatsTotalAmountByMerchantRepository repository;
  private final RedisService redisService;
  private final TracingMetrics metrics;
  private static final String CACHE_PREFIX = "stats:totalamount:merchant:";

  public MerchantStatsTotalAmountByMerchantServiceImpl(MerchantStatsTotalAmountByMerchantRepository repository, RedisService redisService, TracingMetrics metrics) {
    this.repository = repository;
    this.redisService = redisService;
    this.metrics = metrics;
  }

  @Override
  public Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmounts(pb.merchant.Merchant.FindYearMerchantById req) {
    var ctx = metrics.startSpan("MerchantStatsTotalAmountByMerchantService.getMonthlyTotalAmounts");
    String cacheKey = CACHE_PREFIX + "monthly:" + req.getMerchantId() + ":" + req.getYear();

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            List<MerchantStats.MonthAmount> list = new JsonArray(cached).stream()
                .map(o -> MerchantStats.MonthAmount.fromJson((JsonObject) o)).toList();
            return Future.succeededFuture(list);
          }
          return repository.getMonthlyTotalAmountByMerchants(req)
              .onSuccess(list -> {
                JsonArray arr = new JsonArray(list.stream().map(MerchantStats.MonthAmount::toJson).toList());
                redisService.setJson(cacheKey, arr, Duration.ofMinutes(10));
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTotalAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTotalAmounts", e.getMessage()));
  }

  @Override
  public Future<List<MerchantStats.YearAmount>> getYearlyTotalAmounts(pb.merchant.Merchant.FindYearMerchantById req) {
    var ctx = metrics.startSpan("MerchantStatsTotalAmountByMerchantService.getYearlyTotalAmounts");
    String cacheKey = CACHE_PREFIX + "yearly:" + req.getMerchantId() + ":" + req.getYear();

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            List<MerchantStats.YearAmount> list = new JsonArray(cached).stream()
                .map(o -> MerchantStats.YearAmount.fromJson((JsonObject) o)).toList();
            return Future.succeededFuture(list);
          }
          return repository.getYearlyTotalAmountByMerchants(req)
              .onSuccess(list -> {
                JsonArray arr = new JsonArray(list.stream().map(MerchantStats.YearAmount::toJson).toList());
                redisService.setJson(cacheKey, arr, Duration.ofMinutes(10));
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTotalAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTotalAmounts", e.getMessage()));
  }
}
