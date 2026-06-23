package io.example.merchant.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.domain.PagedResult;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.model.MerchantDocumentResponse;
import io.example.merchant.model.MerchantDocumentResponseDeleteAt;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.example.merchant.service.MerchantDocumentQueryService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;

@RequiredArgsConstructor
public class MerchantDocumentQueryServiceImpl implements MerchantDocumentQueryService {
  private static final Logger logger = LoggerFactory.getLogger(MerchantDocumentQueryServiceImpl.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final MerchantDocumentQueryRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;
  private static final String CACHE_PREFIX = "merchant_document:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  private PagedResult<MerchantDocumentResponse> mapDocumentPagination(PagedResult<MerchantDocument> result) {
    List<MerchantDocumentResponse> data = result.getData().stream().map(MerchantDocumentResponse::from).toList();
    return new PagedResult<>(data, result.getTotalRecords());
  }

  private PagedResult<MerchantDocumentResponseDeleteAt> mapDocumentPaginationDeleteAt(
      PagedResult<MerchantDocument> result) {
    List<MerchantDocumentResponseDeleteAt> data = result.getData().stream().map(MerchantDocumentResponseDeleteAt::from)
        .toList();
    return new PagedResult<>(data, result.getTotalRecords());
  }

  private int safePage(int page) {
    return page > 0 ? page : 1;
  }

  private int safePageSize(int size) {
    return size > 0 ? size : 10;
  }

  @Override
  public Future<PagedResult<MerchantDocumentResponse>> findAll(FindAllMerchantDocumentsRequest req) {
    var ctx = tracingMetrics.startSpan("MerchantDocumentQueryService.findAll");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String cacheKey = CACHE_PREFIX + "all:p:" + page + ":s:" + pageSize;

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant_document.cache_hit", true);
              PagedResult<MerchantDocument> typedCached = mapper.readValue(jsonStr,
                  new TypeReference<PagedResult<MerchantDocument>>() {
                  });
              return Future.succeededFuture(mapDocumentPagination(typedCached));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached documents: {}", e.getMessage());
            }
          }
          span.setAttribute("merchant_document.cache_hit", false);
          return repo.findAllDocuments(req)
              .compose(result -> redisService.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(this::mapDocumentPagination);
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findAll", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findAll", e.getMessage()));
  }

  @Override
  public Future<PagedResult<MerchantDocumentResponseDeleteAt>> findByActive(FindAllMerchantDocumentsRequest req) {
    var ctx = tracingMetrics.startSpan("MerchantDocumentQueryService.findByActive");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String cacheKey = CACHE_PREFIX + "active:p:" + page + ":s:" + pageSize;

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant_document.cache_hit", true);
              PagedResult<MerchantDocument> typedCached = mapper.readValue(jsonStr,
                  new TypeReference<PagedResult<MerchantDocument>>() {
                  });
              return Future.succeededFuture(mapDocumentPaginationDeleteAt(typedCached));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached active documents: {}", e.getMessage());
            }
          }
          span.setAttribute("merchant_document.cache_hit", false);
          return repo.findByActiveDocuments(req)
              .compose(result -> redisService.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(this::mapDocumentPaginationDeleteAt);
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findByActive", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findByActive", e.getMessage()));
  }

  @Override
  public Future<PagedResult<MerchantDocumentResponseDeleteAt>> findByTrashed(FindAllMerchantDocumentsRequest req) {
    var ctx = tracingMetrics.startSpan("MerchantDocumentQueryService.findByTrashed");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String cacheKey = CACHE_PREFIX + "trashed:p:" + page + ":s:" + pageSize;

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant_document.cache_hit", true);
              PagedResult<MerchantDocument> typedCached = mapper.readValue(jsonStr,
                  new TypeReference<PagedResult<MerchantDocument>>() {
                  });
              return Future.succeededFuture(mapDocumentPaginationDeleteAt(typedCached));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached trashed documents: {}", e.getMessage());
            }
          }
          span.setAttribute("merchant_document.cache_hit", false);
          return repo.findByTrashedDocuments(req)
              .compose(result -> redisService.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(this::mapDocumentPaginationDeleteAt);
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findByTrashed", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findByTrashed", e.getMessage()));
  }

  @Override
  public Future<MerchantDocumentResponse> findById(Integer documentId) {
    var ctx = tracingMetrics.startSpan("MerchantDocumentQueryService.findById");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "id:" + documentId;

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant_document.cache_hit", true);
              MerchantDocument doc = mapper.readValue(jsonStr, MerchantDocument.class);
              return Future.succeededFuture(MerchantDocumentResponse.from(doc));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached document: {}", e.getMessage());
            }
          }
          span.setAttribute("merchant_document.cache_hit", false);
          return repo.findByIdDocument(documentId)
              .compose(doc -> {
                if (doc == null)
                  return Future.failedFuture(new NotFoundException("Document not found"));
                return redisService.setJson(cacheKey, doc, CACHE_TTL).map(v -> MerchantDocumentResponse.from(doc));
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findById", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findById", e.getMessage()));
  }
}