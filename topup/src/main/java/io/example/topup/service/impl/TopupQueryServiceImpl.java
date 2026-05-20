package io.example.topup.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.domain.PagedResult;
import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.common.model.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.topup.domain.requests.topup.FindAllTopups;
import io.example.topup.model.Topup;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.example.topup.repository.TopupQueryRepository;
import io.example.topup.service.TopupQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import pb.topup.TopupQuery.FindAllTopupRequest;

public class TopupQueryServiceImpl implements TopupQueryService {
  private static final Logger logger = LoggerFactory.getLogger(TopupQueryServiceImpl.class);

  private final TopupQueryRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "topup:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public TopupQueryServiceImpl(
      TopupQueryRepository repo,
      RedisService redisService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.redisService = redisService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponsePagination<List<TopupResponse>>> getTopups(FindAllTopupRequest req) {
    String cacheKey = CACHE_PREFIX + "all:" + req.getPage() + ":" + req.getPageSize() + ":" + req.getSearch();
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("TopupQueryService.getTopups");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("topup.cache_hit", true);
            JsonObject json = new JsonObject(cached);
            List<TopupResponse> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(TopupResponse.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            tracingMetrics.completeSpanSuccess(tracingContext, "get_all", "Success (from cache)");
            return Future.succeededFuture(new ApiResponsePagination<>("success", "Topups fetched successfully (from cache)", data, meta));
          }

          int page = req.getPage() > 0 ? req.getPage() : 1;
          int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
          String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

          logger.info("Fetching topups | search={}, page={}, pageSize={}", keyword, page, pageSize);

          FindAllTopups findReq = FindAllTopups.builder()
              .page(page)
              .pageSize(pageSize)
              .search(keyword)
              .build();

          return repo.getTopups(findReq)
              .compose(result -> {
                ApiResponsePagination<List<TopupResponse>> response = mapTopupPagination(result, page, pageSize);
                return redisService.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL).map(v -> response);
              })
              .onSuccess(response -> {
                span.setAttribute("topups.count", (long) response.data().size());
                span.setAttribute("topups.total_records", (long) response.pagination().totalRecords());
                tracingMetrics.completeSpanSuccess(tracingContext, "get_all", "Topups fetched successfully");
              });
        })
        .recover(throwable -> {
          logger.error("Failed to fetch topups", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_all", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch topups: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<TopupResponse>>> getActiveTopups(FindAllTopupRequest req) {
    String cacheKey = CACHE_PREFIX + "active:" + req.getPage() + ":" + req.getPageSize() + ":" + req.getSearch();
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("TopupQueryService.getActiveTopups");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("topup.cache_hit", true);
            JsonObject json = new JsonObject(cached);
            List<TopupResponse> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(TopupResponse.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            tracingMetrics.completeSpanSuccess(tracingContext, "get_active", "Success (from cache)");
            return Future.succeededFuture(new ApiResponsePagination<>("success", "Active topups fetched successfully (from cache)", data, meta));
          }

          int page = req.getPage() > 0 ? req.getPage() : 1;
          int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
          String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

          logger.info("Fetching active topups | search={}, page={}, pageSize={}", keyword, page, pageSize);

          FindAllTopups findReq = FindAllTopups.builder()
              .page(page)
              .pageSize(pageSize)
              .search(keyword)
              .build();

          return repo.getActiveTopups(findReq)
              .compose(result -> {
                ApiResponsePagination<List<TopupResponse>> response = mapTopupPagination(result, page, pageSize);
                return redisService.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL).map(v -> response);
              })
              .onSuccess(response -> {
                span.setAttribute("topups.count", (long) response.data().size());
                span.setAttribute("topups.total_records", (long) response.pagination().totalRecords());
                tracingMetrics.completeSpanSuccess(tracingContext, "get_active", "Active topups fetched successfully");
              });
        })
        .recover(throwable -> {
          logger.error("Failed to fetch active topups", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_active", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch active topups: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<TopupResponseDeleteAt>>> getTrashedTopups(FindAllTopupRequest req) {
    String cacheKey = CACHE_PREFIX + "trashed:" + req.getPage() + ":" + req.getPageSize() + ":" + req.getSearch();
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("TopupQueryService.getTrashedTopups");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("topup.cache_hit", true);
            JsonObject json = new JsonObject(cached);
            List<TopupResponseDeleteAt> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(TopupResponseDeleteAt.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            tracingMetrics.completeSpanSuccess(tracingContext, "get_trashed", "Success (from cache)");
            return Future.succeededFuture(new ApiResponsePagination<>("success", "Trashed topups fetched successfully (from cache)", data, meta));
          }

          int page = req.getPage() > 0 ? req.getPage() : 1;
          int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
          String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

          logger.info("Fetching trashed topups | search={}, page={}, pageSize={}", keyword, page, pageSize);

          FindAllTopups findReq = FindAllTopups.builder()
              .page(page)
              .pageSize(pageSize)
              .search(keyword)
              .build();

          return repo.getTrashedTopups(findReq)
              .compose(result -> {
                ApiResponsePagination<List<TopupResponseDeleteAt>> response = mapTopupPaginationDeleteAt(result, page, pageSize);
                return redisService.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL).map(v -> response);
              })
              .onSuccess(response -> {
                span.setAttribute("topups.count", (long) response.data().size());
                span.setAttribute("topups.total_records", (long) response.pagination().totalRecords());
                tracingMetrics.completeSpanSuccess(tracingContext, "get_trashed", "Trashed topups fetched successfully");
              });
        })
        .recover(throwable -> {
          logger.error("Failed to fetch trashed topups", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_trashed", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch trashed topups: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<TopupResponse>> getTopupById(Integer topupId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TopupQueryService.getTopupById",
        Attributes.builder()
            .put("topup.id", (long) topupId)
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Fetching topup by id: {}", topupId);
    String cacheKey = CACHE_PREFIX + topupId;

    return redisService.get(cacheKey)
        .compose(cachedTopup -> {
          if (cachedTopup != null && !cachedTopup.isEmpty()) {
            logger.info("Topup {} found in cache", topupId);
            span.setAttribute("topup.cache_hit", true);
            try {
              Topup topup = Topup.fromJson(new JsonObject(cachedTopup));
              tracingMetrics.completeSpanSuccess(tracingContext, "get_by_id", "Topup fetched from cache");
              return Future.succeededFuture(ApiResponse.success(
                  "Topup fetched successfully (from cache)",
                  TopupResponse.from(topup)));
            } catch (Exception e) {
              logger.warn("Failed to parse cached topup data for topup {}: {}", topupId, e.getMessage());
              return fetchTopupFromDatabase(topupId, tracingContext);
            }
          } else {
            span.setAttribute("topup.cache_hit", false);
            return fetchTopupFromDatabase(topupId, tracingContext);
          }
        })
        .recover(err -> {
          logger.error("Failed to fetch topup by id: {}", topupId, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_id", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.error("Failed to fetch topup: " + err.getMessage()));
        });
  }

  private Future<ApiResponse<TopupResponse>> fetchTopupFromDatabase(Integer topupId,
      TracingMetrics.TracingContext tracingContext) {
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    return repo.getTopupById(topupId)
        .compose((Topup topup) -> {
          if (topup == null) {
            return Future.failedFuture(new NotFoundException("Topup not found"));
          }

          span.setAttribute("topup.card_number", Objects.requireNonNull(topup.getCardNumber()));

          String cacheKey = CACHE_PREFIX + topupId;
          redisService.setJson(cacheKey, topup.toJson(), CACHE_TTL)
              .onSuccess(v -> logger.debug("Topup {} cached successfully", topupId))
              .onFailure(err -> logger.warn("Failed to cache topup {}: {}", topupId, err.getMessage()));

          return Future.succeededFuture(ApiResponse.success(
              "Topup fetched successfully",
              TopupResponse.from(topup)));
        });
  }

  private ApiResponsePagination<List<TopupResponse>> mapTopupPagination(
      PagedResult<Topup> result,
      int page,
      int pageSize) {

    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<TopupResponse> data = result.getData()
        .stream()
        .map(TopupResponse::from)
        .toList();

    return new ApiResponsePagination<>(
        "success",
        "Topups found",
        data,
        new PaginationMeta(
            page,
            pageSize,
            totalPages,
            totalRecords));
  }

  private ApiResponsePagination<List<TopupResponseDeleteAt>> mapTopupPaginationDeleteAt(
      PagedResult<Topup> result,
      int page,
      int pageSize) {

    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<TopupResponseDeleteAt> data = result.getData()
        .stream()
        .map(TopupResponseDeleteAt::from)
        .toList();

    return new ApiResponsePagination<>(
        "success",
        "Trashed topups found",
        data,
        new PaginationMeta(
            page,
            pageSize,
            totalPages,
            totalRecords));
  }
}
