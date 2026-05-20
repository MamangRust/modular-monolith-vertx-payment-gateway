package io.example.withdraw.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.domain.PagedResult;
import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.common.model.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.withdraw.domain.requests.FindAllWithdraws;
import io.example.withdraw.model.Withdraw;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.example.withdraw.repository.WithdrawQueryRepository;
import io.example.withdraw.service.WithdrawQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pb.withdraw.Withdraw.FindAllWithdrawByCardNumberRequest;
import pb.withdraw.Withdraw.FindAllWithdrawRequest;

public class WithdrawQueryServiceImpl implements WithdrawQueryService {
  private static final Logger logger = LoggerFactory.getLogger(WithdrawQueryServiceImpl.class);

  private final WithdrawQueryRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "withdraw:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public WithdrawQueryServiceImpl(
      WithdrawQueryRepository repo,
      RedisService redisService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.redisService = redisService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponsePagination<List<WithdrawResponse>>> getWithdraws(FindAllWithdrawRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("WithdrawQueryService.getWithdraws");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    String cacheKey = CACHE_PREFIX + "list:" + keyword + ":" + page + ":" + pageSize;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            JsonObject json = new JsonObject(cached);
            List<WithdrawResponse> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(WithdrawResponse.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            return Future.succeededFuture(new ApiResponsePagination<>("success", "Withdrawals found (cached)", data, meta));
          }
          span.setAttribute("cache.hit", false);
          FindAllWithdraws findAllReq = FindAllWithdraws.builder()
              .search(keyword)
              .page(page)
              .pageSize(pageSize)
              .build();

          return repo.getWithdraws(findAllReq)
              .map(result -> {
                ApiResponsePagination<List<WithdrawResponse>> response = mapWithdrawPagination(result, page, pageSize);
                redisService.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL);
                return response;
              });
        })
        .onSuccess(response -> {
          if (response.data() != null) {
            span.setAttribute("withdraws.count", (long) response.data().size());
          }
          tracingMetrics.completeSpanSuccess(tracingContext, "get_all", "Withdrawals fetched successfully");
        })
        .recover(throwable -> {
          logger.error("Failed to fetch withdrawals", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_all", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch withdrawals: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<WithdrawResponse>>> getWithdrawsByCardNumber(
      FindAllWithdrawByCardNumberRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("WithdrawQueryService.getWithdrawsByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";
    String cardNumber = req.getCardNumber();

    String cacheKey = CACHE_PREFIX + "card:" + cardNumber + ":" + keyword + ":" + page + ":" + pageSize;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            JsonObject json = new JsonObject(cached);
            List<WithdrawResponse> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(WithdrawResponse.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            return Future.succeededFuture(new ApiResponsePagination<>("success", "Withdrawals for card found (cached)", data, meta));
          }
          span.setAttribute("cache.hit", false);
          return repo.getWithdrawsByCardNumber(cardNumber, keyword, page, pageSize)
              .map(result -> {
                ApiResponsePagination<List<WithdrawResponse>> response = mapWithdrawPagination(result, page, pageSize);
                redisService.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL);
                return response;
              });
        })
        .onSuccess(response -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card",
              "Withdrawals for card fetched successfully");
        })
        .recover(throwable -> {
          logger.error("Failed to fetch withdrawals for card: {}", cardNumber, throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_by_card", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch withdrawals for card: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<WithdrawResponseDeleteAt>>> getActiveWithdraws(FindAllWithdrawRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("WithdrawQueryService.getActiveWithdraws");
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    FindAllWithdraws findAllReq = FindAllWithdraws.builder()
        .search(keyword)
        .page(page)
        .pageSize(pageSize)
        .build();

    return repo.getActiveWithdraws(findAllReq)
        .map(result -> {
          ApiResponsePagination<List<WithdrawResponseDeleteAt>> response = mapWithdrawPaginationDeleteAt(result, page,
              pageSize);
          tracingMetrics.completeSpanSuccess(tracingContext, "get_active", "Active withdrawals fetched successfully");
          return response;
        })
        .recover(throwable -> {
          tracingMetrics.completeSpanError(tracingContext, "get_active", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch active withdrawals: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<WithdrawResponseDeleteAt>>> getTrashedWithdraws(FindAllWithdrawRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("WithdrawQueryService.getTrashedWithdraws");
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    FindAllWithdraws findAllReq = FindAllWithdraws.builder()
        .search(keyword)
        .page(page)
        .pageSize(pageSize)
        .build();

    return repo.getTrashedWithdraws(findAllReq)
        .map(result -> {
          ApiResponsePagination<List<WithdrawResponseDeleteAt>> response = mapWithdrawPaginationDeleteAt(result, page,
              pageSize);
          tracingMetrics.completeSpanSuccess(tracingContext, "get_trashed", "Trashed withdrawals fetched successfully");
          return response;
        })
        .recover(throwable -> {
          tracingMetrics.completeSpanError(tracingContext, "get_trashed", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch trashed withdrawals: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<WithdrawResponse>> getWithdrawById(Integer withdrawId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "WithdrawQueryService.getWithdrawById",
        Attributes.builder()
            .put("withdraw.id", (long) withdrawId)
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    String cacheKey = CACHE_PREFIX + withdrawId;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            Withdraw withdraw = Withdraw.fromJson(new JsonObject(cached));
            tracingMetrics.completeSpanSuccess(tracingContext, "get_by_id", "Withdraw fetched from cache");
            return Future.succeededFuture(ApiResponse.success(
                "Withdraw fetched successfully (from cache)",
                WithdrawResponse.from(withdraw)));
          }
          span.setAttribute("cache.hit", false);
          return repo.getWithdrawById(withdrawId)
              .compose(withdraw -> {
                if (withdraw == null) {
                  return Future.failedFuture(new NotFoundException("Withdrawal not found"));
                }
                redisService.setJson(cacheKey, withdraw.toJson(), CACHE_TTL);
                tracingMetrics.completeSpanSuccess(tracingContext, "get_by_id", "Withdraw fetched from DB");
                return Future.succeededFuture(ApiResponse.success(
                    "Withdraw fetched successfully",
                    WithdrawResponse.from(withdraw)));
              });
        })
        .recover(err -> {
          tracingMetrics.completeSpanError(tracingContext, "get_by_id", err.getMessage());
          return Future.succeededFuture(ApiResponse.error(err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<List<WithdrawResponse>>> getWithdrawsByCardNumberPrimitive(String cardNumber) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "WithdrawQueryService.getWithdrawsByCardNumberPrimitive",
        Attributes.builder()
            .put("card.number", Objects.requireNonNull(cardNumber))
            .build());

    String cacheKey = CACHE_PREFIX + "card_primitive:" + cardNumber;

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            JsonArray arr = new JsonArray(cached);
            List<WithdrawResponse> data = arr.stream()
                .map(o -> ((JsonObject) o).mapTo(WithdrawResponse.class))
                .collect(Collectors.toList());
            tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card_primitive", "Withdrawals found (cache)");
            return Future.succeededFuture(ApiResponse.success("Withdrawals found", data));
          }
          return repo.getWithdrawsByCardNumberPrimitive(cardNumber)
              .map(list -> {
                List<WithdrawResponse> data = list.stream().map(WithdrawResponse::from).toList();
                redisService.setJson(cacheKey, new JsonArray(data.stream().map(JsonObject::mapFrom).toList()),
                    CACHE_TTL);
                tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card_primitive", "Withdrawals found (DB)");
                return ApiResponse.success("Withdrawals found", data);
              });
        })
        .recover(err -> {
          tracingMetrics.completeSpanError(tracingContext, "get_by_card_primitive", err.getMessage());
          return Future.succeededFuture(ApiResponse.error(err.getMessage()));
        });
  }

  private ApiResponsePagination<List<WithdrawResponse>> mapWithdrawPagination(
      PagedResult<Withdraw> result,
      int page,
      int pageSize) {

    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<WithdrawResponse> data = result.getData()
        .stream()
        .map(WithdrawResponse::from)
        .toList();

    return new ApiResponsePagination<>(
        "success",
        "Withdrawals found",
        data,
        new PaginationMeta(
            page,
            pageSize,
            totalPages,
            totalRecords));
  }

  private ApiResponsePagination<List<WithdrawResponseDeleteAt>> mapWithdrawPaginationDeleteAt(
      PagedResult<Withdraw> result,
      int page,
      int pageSize) {

    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<WithdrawResponseDeleteAt> data = result.getData()
        .stream()
        .map(WithdrawResponseDeleteAt::from)
        .toList();

    return new ApiResponsePagination<>(
        "success",
        "Trashed withdrawals found",
        data,
        new PaginationMeta(
            page,
            pageSize,
            totalPages,
            totalRecords));
  }
}
