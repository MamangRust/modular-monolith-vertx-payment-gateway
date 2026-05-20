package io.example.merchant.service.impl;

import java.util.List;
import java.time.Duration;

import io.example.common.domain.PagedResult;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.common.model.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.model.MerchantDocumentResponse;
import io.example.merchant.model.MerchantDocumentResponseDeleteAt;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.example.merchant.service.MerchantDocumentQueryService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;

public class MerchantDocumentQueryServiceImpl implements MerchantDocumentQueryService {
  private final MerchantDocumentQueryRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;
  private static final String CACHE_PREFIX = "merchant_document:";

  public MerchantDocumentQueryServiceImpl(
      MerchantDocumentQueryRepository repo,
      RedisService redisService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.redisService = redisService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponsePagination<List<MerchantDocumentResponse>>> findAll(FindAllMerchantDocumentsRequest req) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantDocumentQueryService.findAll");
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String cacheKey = CACHE_PREFIX + "all:" + page + ":" + pageSize;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            JsonObject json = new JsonObject(cached);
            List<MerchantDocumentResponse> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(MerchantDocumentResponse.class)).toList();
            PaginationMeta meta = json.getJsonObject("meta").mapTo(PaginationMeta.class);
            return Future
                .succeededFuture(new ApiResponsePagination<>("success", "Documents found (cached)", data, meta));
          }
          return repo.findAllDocuments(req)
              .map(result -> {
                ApiResponsePagination<List<MerchantDocumentResponse>> response = mapDocumentPagination(result, page,
                    pageSize);
                JsonObject cacheData = new JsonObject()
                    .put("data", new JsonArray(response.data().stream().map(JsonObject::mapFrom).toList()))
                    .put("meta", JsonObject.mapFrom(response.pagination()));
                redisService.setJson(cacheKey, cacheData, Duration.ofMinutes(10));
                return response;
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findAll", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findAll", e.getMessage()));
  }

  @Override
  public Future<ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>>> findByActive(
      FindAllMerchantDocumentsRequest req) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantDocumentQueryService.findByActive");
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String cacheKey = CACHE_PREFIX + "active:" + page + ":" + pageSize;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            JsonObject json = new JsonObject(cached);
            List<MerchantDocumentResponseDeleteAt> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(MerchantDocumentResponseDeleteAt.class)).toList();
            PaginationMeta meta = json.getJsonObject("meta").mapTo(PaginationMeta.class);
            return Future
                .succeededFuture(new ApiResponsePagination<>("success", "Documents found (cached)", data, meta));
          }
          return repo.findByActiveDocuments(req)
              .map(result -> {
                ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>> response = mapDocumentPaginationDeleteAt(
                    result, page, pageSize);
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
  public Future<ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>>> findByTrashed(
      FindAllMerchantDocumentsRequest req) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantDocumentQueryService.findByTrashed");
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String cacheKey = CACHE_PREFIX + "trashed:" + page + ":" + pageSize;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            JsonObject json = new JsonObject(cached);
            List<MerchantDocumentResponseDeleteAt> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(MerchantDocumentResponseDeleteAt.class)).toList();
            PaginationMeta meta = json.getJsonObject("meta").mapTo(PaginationMeta.class);
            return Future
                .succeededFuture(new ApiResponsePagination<>("success", "Documents found (cached)", data, meta));
          }
          return repo.findByTrashedDocuments(req)
              .map(result -> {
                ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>> response = mapDocumentPaginationDeleteAt(
                    result, page, pageSize);
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
  public Future<ApiResponse<MerchantDocumentResponse>> findById(int documentId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantDocumentQueryService.findById");
    String cacheKey = CACHE_PREFIX + documentId;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            MerchantDocument doc = MerchantDocument.fromJson(new JsonObject(cached));
            return Future.succeededFuture(
                ApiResponse.success("Document fetched from cache", MerchantDocumentResponse.from(doc)));
          }
          return repo.findByIdDocument(documentId)
              .compose(doc -> {
                if (doc == null)
                  return Future.failedFuture("Document not found");
                redisService.setJson(cacheKey, doc.toJson(), Duration.ofMinutes(60));
                return Future.succeededFuture(
                    ApiResponse.success("Document fetched successfully", MerchantDocumentResponse.from(doc)));
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findById", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findById", e.getMessage()));
  }

  private ApiResponsePagination<List<MerchantDocumentResponse>> mapDocumentPagination(
      PagedResult<MerchantDocument> result, int page, int pageSize) {
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<MerchantDocumentResponse> data = result.getData().stream().map(MerchantDocumentResponse::from).toList();
    return new ApiResponsePagination<>("success", "Documents found", data,
        new PaginationMeta(page, pageSize, totalPages, totalRecords));
  }

  private ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>> mapDocumentPaginationDeleteAt(
      PagedResult<MerchantDocument> result, int page, int pageSize) {
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<MerchantDocumentResponseDeleteAt> data = result.getData().stream().map(MerchantDocumentResponseDeleteAt::from)
        .toList();
    return new ApiResponsePagination<>("success", "Documents found", data,
        new PaginationMeta(page, pageSize, totalPages, totalRecords));
  }
}
