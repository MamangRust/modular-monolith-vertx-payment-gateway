package io.example.merchant.service.impl;

import java.util.List;
import java.time.Duration;
import io.example.common.model.ApiResponsePagination;
import io.example.common.model.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.model.MerchantTransactions;
import io.example.merchant.repository.MerchantTransactionRepository;
import io.example.merchant.service.MerchantTransactionService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;
import pb.merchant.Merchant.FindAllMerchantTransactionId;

public class MerchantTransactionServiceImpl implements MerchantTransactionService {
  private final MerchantTransactionRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;
  private static final String CACHE_PREFIX = "transactions:";

  public MerchantTransactionServiceImpl(MerchantTransactionRepository repo, RedisService redisService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.redisService = redisService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponsePagination<List<MerchantTransactions>>> getTransactions(FindAllMerchantTransaction req) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantTransactionService.getTransactions");
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String cacheKey = CACHE_PREFIX + "all:" + page + ":" + pageSize + ":"
        + (req.getSearch() != null ? req.getSearch() : "none");

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            JsonObject json = new JsonObject(cached);
            List<MerchantTransactions> list = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(MerchantTransactions.class)).toList();
            int total = json.getInteger("total");
            int totalPages = (pageSize > 0) ? (int) Math.ceil((double) total / pageSize) : 0;
            return Future.succeededFuture(new ApiResponsePagination<>("success", "Transactions fetched from cache",
                list, new PaginationMeta(page, pageSize, totalPages, total)));
          }
          return repo.findAllTransactionMerchant(req)
              .onSuccess(res -> {
                JsonObject cacheData = new JsonObject()
                    .put("data", new JsonArray(res.getData().stream().map(JsonObject::mapFrom).toList()))
                    .put("total", res.getTotalRecords());
                redisService.setJson(cacheKey, cacheData, Duration.ofMinutes(5));
              })
              .map(res -> {
                int total = res.getTotalRecords();
                int totalPages = (pageSize > 0) ? (int) Math.ceil((double) total / pageSize) : 0;
                return new ApiResponsePagination<>("success", "Transactions fetched", res.getData(),
                    new PaginationMeta(page, pageSize, totalPages, total));
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getTransactions", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getTransactions", e.getMessage()));
  }

  @Override
  public Future<ApiResponsePagination<List<MerchantTransactions>>> getTransactionsByApiKey(
      FindAllMerchantTransactionApikey req) {
    TracingMetrics.TracingContext ctx = tracingMetrics
        .startSpan("MerchantTransactionService.getTransactionsByApiKey");
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String cacheKey = CACHE_PREFIX + "apikey:" + req.getApiKey() + ":" + page + ":" + pageSize + ":"
        + (req.getSearch() != null ? req.getSearch() : "none");

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            JsonObject json = new JsonObject(cached);
            List<MerchantTransactions> list = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(MerchantTransactions.class)).toList();
            int total = json.getInteger("total");
            int totalPages = (pageSize > 0) ? (int) Math.ceil((double) total / pageSize) : 0;
            return Future.succeededFuture(new ApiResponsePagination<>("success", "Transactions fetched from cache",
                list, new PaginationMeta(page, pageSize, totalPages, total)));
          }
          return repo.findAllTransactionByApikey(req)
              .onSuccess(res -> {
                JsonObject cacheData = new JsonObject()
                    .put("data", new JsonArray(res.getData().stream().map(JsonObject::mapFrom).toList()))
                    .put("total", res.getTotalRecords());
                redisService.setJson(cacheKey, cacheData, Duration.ofMinutes(5));
              })
              .map(res -> {
                int total = res.getTotalRecords();
                int totalPages = (pageSize > 0) ? (int) Math.ceil((double) total / pageSize) : 0;
                return new ApiResponsePagination<>("success", "Transactions fetched", res.getData(),
                    new PaginationMeta(page, pageSize, totalPages, total));
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getTransactionsByApiKey", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getTransactionsByApiKey", e.getMessage()));
  }

  @Override
  public Future<ApiResponsePagination<List<MerchantTransactions>>> getTransactionsByMerchantId(
      FindAllMerchantTransactionId req) {
    TracingMetrics.TracingContext ctx = tracingMetrics
        .startSpan("MerchantTransactionService.getTransactionsByMerchantId");
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String cacheKey = CACHE_PREFIX + "merchant:" + req.getId() + ":" + page + ":" + pageSize + ":"
        + (req.getSearch() != null ? req.getSearch() : "none");

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            JsonObject json = new JsonObject(cached);
            List<MerchantTransactions> list = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(MerchantTransactions.class)).toList();
            int total = json.getInteger("total");
            int totalPages = (pageSize > 0) ? (int) Math.ceil((double) total / pageSize) : 0;
            return Future.succeededFuture(new ApiResponsePagination<>("success", "Transactions fetched from cache",
                list, new PaginationMeta(page, pageSize, totalPages, total)));
          }
          return repo.findAllTransactionByMerchant(req)
              .onSuccess(res -> {
                JsonObject cacheData = new JsonObject()
                    .put("data", new JsonArray(res.getData().stream().map(JsonObject::mapFrom).toList()))
                    .put("total", res.getTotalRecords());
                redisService.setJson(cacheKey, cacheData, Duration.ofMinutes(5));
              })
              .map(res -> {
                int total = res.getTotalRecords();
                int totalPages = (pageSize > 0) ? (int) Math.ceil((double) total / pageSize) : 0;
                return new ApiResponsePagination<>("success", "Transactions fetched", res.getData(),
                    new PaginationMeta(page, pageSize, totalPages, total));
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getTransactionsByMerchantId", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getTransactionsByMerchantId", e.getMessage()));
  }
}
