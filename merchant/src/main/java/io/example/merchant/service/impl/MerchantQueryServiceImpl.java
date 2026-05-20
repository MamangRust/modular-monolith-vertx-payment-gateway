package io.example.merchant.service.impl;

import java.time.Duration;
import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.common.model.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.exception.NotFoundException;
import io.example.merchant.model.Merchant;
import io.example.merchant.model.MerchantResponse;
import io.example.merchant.model.MerchantResponseDeleteAt;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.service.MerchantQueryService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pb.merchant.Merchant.FindAllMerchantRequest;

public class MerchantQueryServiceImpl implements MerchantQueryService {
  private final MerchantQueryRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;
  private static final String CACHE_PREFIX = "merchant:";

  public MerchantQueryServiceImpl(
      MerchantQueryRepository repo,
      RedisService redisService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.redisService = redisService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponsePagination<List<MerchantResponse>>> findAll(FindAllMerchantRequest req) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantQueryService.findAll");
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String cacheKey = CACHE_PREFIX + "all:" + page + ":" + pageSize + ":"
        + (req.getSearch() != null ? req.getSearch() : "none");

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            JsonObject json = new JsonObject(cached);
            List<MerchantResponse> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(MerchantResponse.class)).toList();
            PaginationMeta meta = json.getJsonObject("meta").mapTo(PaginationMeta.class);
            return Future
                .succeededFuture(new ApiResponsePagination<>("success", "Merchants found (cached)", data, meta));
          }
          return repo.findAllMerchants(req)
              .map(result -> {
                ApiResponsePagination<List<MerchantResponse>> response = mapMerchantPagination(result, page, pageSize);
                JsonObject cacheData = new JsonObject()
                    .put("data", new JsonArray(response.data().stream().map(JsonObject::mapFrom).toList()))
                    .put("meta", JsonObject.mapFrom(response.pagination()));
                redisService.setJson(cacheKey, cacheData, Duration.ofMinutes(10));
                return response;
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findAll", "Merchants fetched successfully"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findAll", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<MerchantResponse>> findById(int merchantId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantQueryService.findById",
        Attributes.builder().put("merchant.id", (long) merchantId).build());
    String cacheKey = CACHE_PREFIX + merchantId;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            Merchant merchant = Merchant.fromJson(new JsonObject(cached));
            return Future
                .succeededFuture(ApiResponse.success("Merchant fetched from cache", MerchantResponse.from(merchant)));
          }
          return repo.findByMerchantId(merchantId)
              .compose(merchant -> {
                if (merchant == null)
                  return Future.failedFuture(new NotFoundException("Merchant not found"));
                redisService.setJson(cacheKey, merchant.toJson(), Duration.ofMinutes(60));
                return Future.succeededFuture(
                    ApiResponse.success("Merchant fetched successfully", MerchantResponse.from(merchant)));
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findById", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findById", e.getMessage()));
  }

  @Override
  public Future<ApiResponsePagination<List<MerchantResponseDeleteAt>>> findByActive(FindAllMerchantRequest req) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantQueryService.findByActive");
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String cacheKey = CACHE_PREFIX + "active:" + page + ":" + pageSize + ":"
        + (req.getSearch() != null ? req.getSearch() : "none");

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            JsonObject json = new JsonObject(cached);
            List<MerchantResponseDeleteAt> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(MerchantResponseDeleteAt.class)).toList();
            PaginationMeta meta = json.getJsonObject("meta").mapTo(PaginationMeta.class);
            return Future
                .succeededFuture(new ApiResponsePagination<>("success", "Active merchants found (cached)", data, meta));
          }
          return repo.findByActive(req)
              .map(result -> {
                ApiResponsePagination<List<MerchantResponseDeleteAt>> response = mapMerchantPaginationDeleteAt(result,
                    page, pageSize);
                JsonObject cacheData = new JsonObject()
                    .put("data", new JsonArray(response.data().stream().map(JsonObject::mapFrom).toList()))
                    .put("meta", JsonObject.mapFrom(response.pagination()));
                redisService.setJson(cacheKey, cacheData, Duration.ofMinutes(10));
                return response;
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findByActive", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findByActive", e.getMessage()));
  }

  @Override
  public Future<ApiResponsePagination<List<MerchantResponseDeleteAt>>> findByTrashed(FindAllMerchantRequest req) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantQueryService.findByTrashed");
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String cacheKey = CACHE_PREFIX + "trashed:" + page + ":" + pageSize + ":"
        + (req.getSearch() != null ? req.getSearch() : "none");

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            JsonObject json = new JsonObject(cached);
            List<MerchantResponseDeleteAt> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(MerchantResponseDeleteAt.class)).toList();
            PaginationMeta meta = json.getJsonObject("meta").mapTo(PaginationMeta.class);
            return Future.succeededFuture(
                new ApiResponsePagination<>("success", "Trashed merchants found (cached)", data, meta));
          }
          return repo.findByTrashed(req)
              .map(result -> {
                ApiResponsePagination<List<MerchantResponseDeleteAt>> response = mapMerchantPaginationDeleteAt(result,
                    page, pageSize);
                JsonObject cacheData = new JsonObject()
                    .put("data", new JsonArray(response.data().stream().map(JsonObject::mapFrom).toList()))
                    .put("meta", JsonObject.mapFrom(response.pagination()));
                redisService.setJson(cacheKey, cacheData, Duration.ofMinutes(10));
                return response;
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findByTrashed", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findByTrashed", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<MerchantResponse>> findByApiKey(String apiKey) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantQueryService.findByApiKey");
    String cacheKey = CACHE_PREFIX + "apikey:" + apiKey;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            Merchant merchant = Merchant.fromJson(new JsonObject(cached));
            return Future
                .succeededFuture(ApiResponse.success("Merchant fetched from cache", MerchantResponse.from(merchant)));
          }
          return repo.findByApiKey(apiKey)
              .compose(merchant -> {
                if (merchant == null)
                  return Future.succeededFuture(ApiResponse.<MerchantResponse>error("Merchant not found"));
                redisService.setJson(cacheKey, merchant.toJson(), Duration.ofMinutes(60));
                return Future.succeededFuture(
                    ApiResponse.success("Merchant fetched successfully", MerchantResponse.from(merchant)));
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findByApiKey", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findByApiKey", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<List<MerchantResponse>>> findByMerchantUserId(int userId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantQueryService.findByMerchantUserId");
    String cacheKey = CACHE_PREFIX + "user:" + userId;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            List<MerchantResponse> list = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(MerchantResponse.class)).toList();
            return Future.succeededFuture(ApiResponse.success("Merchants fetched from cache", list));
          }
          return repo.findByMerchantUserId(userId)
              .onSuccess(list -> {
                JsonArray arr = new JsonArray(
                    list.stream().map(m -> JsonObject.mapFrom(MerchantResponse.from(m))).toList());
                redisService.setJson(cacheKey, arr, Duration.ofMinutes(10));
              })
              .map(list -> ApiResponse.success("Merchants fetched successfully",
                  list.stream().map(MerchantResponse::from).toList()));
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findByMerchantUserId", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findByMerchantUserId", e.getMessage()));
  }

  private ApiResponsePagination<List<MerchantResponse>> mapMerchantPagination(PagedResult<Merchant> result, int page,
      int pageSize) {
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<MerchantResponse> data = result.getData().stream().map(MerchantResponse::from).toList();
    return new ApiResponsePagination<>("success", "Merchants found", data,
        new PaginationMeta(page, pageSize, totalPages, totalRecords));
  }

  private ApiResponsePagination<List<MerchantResponseDeleteAt>> mapMerchantPaginationDeleteAt(
      PagedResult<Merchant> result, int page, int pageSize) {
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<MerchantResponseDeleteAt> data = result.getData().stream().map(MerchantResponseDeleteAt::from).toList();
    return new ApiResponsePagination<>("success", "Merchants found", data,
        new PaginationMeta(page, pageSize, totalPages, totalRecords));
  }
}
