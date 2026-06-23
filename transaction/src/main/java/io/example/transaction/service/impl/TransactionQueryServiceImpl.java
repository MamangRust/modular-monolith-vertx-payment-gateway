package io.example.transaction.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.domain.ApiResponse;
import io.example.common.domain.ApiResponsePagination;
import io.example.common.domain.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.domain.requests.FindAllTransactionCardNumber;
import io.example.transaction.domain.requests.FindAllTransactions;
import io.example.transaction.model.Transaction;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.example.transaction.repository.TransactionQueryRepository;
import io.example.transaction.service.TransactionQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest;
import pb.transaction.TransactionQuery.FindAllTransactionRequest;

public class TransactionQueryServiceImpl implements TransactionQueryService {
  private static final Logger logger = LoggerFactory.getLogger(TransactionQueryServiceImpl.class);

  private final TransactionQueryRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "transaction:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public TransactionQueryServiceImpl(
      TransactionQueryRepository repo,
      RedisService redisService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.redisService = redisService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponsePagination<List<TransactionResponse>>> getTransactions(FindAllTransactionRequest req) {
    String cacheKey = CACHE_PREFIX + "all:" + req.getPage() + ":" + req.getPageSize() + ":" + req.getSearch();
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("TransactionQueryService.getTransactions");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("transaction.cache_hit", true);
            JsonObject json = new JsonObject(cached);
            List<TransactionResponse> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionResponse.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            tracingMetrics.completeSpanSuccess(tracingContext, "get_all", "Success (from cache)");
            return Future.succeededFuture(
                new ApiResponsePagination<>("success", "Transactions fetched successfully (from cache)", data, meta));
          }

          int page = req.getPage() > 0 ? req.getPage() : 1;
          int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
          String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

          FindAllTransactions findAllReq = FindAllTransactions.builder()
              .search(keyword)
              .page(page)
              .pageSize(pageSize)
              .build();

          return repo.getTransactions(findAllReq)
              .compose(result -> {
                ApiResponsePagination<List<TransactionResponse>> response = mapTransactionPagination(result, page,
                    pageSize);
                return redisService.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL).map(v -> response);
              })
              .onSuccess(response -> {
                span.setAttribute("transactions.count", (long) response.data().size());
                span.setAttribute("transactions.total_records", (long) response.pagination().totalRecords());
                tracingMetrics.completeSpanSuccess(tracingContext, "get_all", "Transactions fetched successfully");
              });
        })
        .recover(throwable -> {
          logger.error("Failed to fetch transactions", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_all", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch transactions: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<TransactionResponseDeleteAt>>> getActiveTransactions(
      FindAllTransactionRequest req) {
    String cacheKey = CACHE_PREFIX + "active:" + req.getPage() + ":" + req.getPageSize() + ":" + req.getSearch();
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("TransactionQueryService.getActiveTransactions");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("transaction.cache_hit", true);
            JsonObject json = new JsonObject(cached);
            List<TransactionResponseDeleteAt> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionResponseDeleteAt.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            tracingMetrics.completeSpanSuccess(tracingContext, "get_active", "Success (from cache)");
            return Future.succeededFuture(new ApiResponsePagination<>("success",
                "Active transactions fetched successfully (from cache)", data, meta));
          }

          int page = req.getPage() > 0 ? req.getPage() : 1;
          int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
          String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

          FindAllTransactions findAllReq = FindAllTransactions.builder()
              .search(keyword)
              .page(page)
              .pageSize(pageSize)
              .build();

          return repo.getActiveTransactions(findAllReq)
              .compose(result -> {
                ApiResponsePagination<List<TransactionResponseDeleteAt>> response = mapTransactionPaginationDeleteAt(
                    result, page, pageSize);
                return redisService.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL).map(v -> response);
              })
              .onSuccess(response -> {
                span.setAttribute("transactions.count", (long) response.data().size());
                span.setAttribute("transactions.total_records", (long) response.pagination().totalRecords());
                tracingMetrics.completeSpanSuccess(tracingContext, "get_active",
                    "Active transactions fetched successfully");
              });
        })
        .recover(throwable -> {
          logger.error("Failed to fetch active transactions", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_active", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch active transactions: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<TransactionResponseDeleteAt>>> getTrashedTransactions(
      FindAllTransactionRequest req) {
    String cacheKey = CACHE_PREFIX + "trashed:" + req.getPage() + ":" + req.getPageSize() + ":" + req.getSearch();
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("TransactionQueryService.getTrashedTransactions");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("transaction.cache_hit", true);
            JsonObject json = new JsonObject(cached);
            List<TransactionResponseDeleteAt> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionResponseDeleteAt.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            tracingMetrics.completeSpanSuccess(tracingContext, "get_trashed", "Success (from cache)");
            return Future.succeededFuture(new ApiResponsePagination<>("success",
                "Trashed transactions fetched successfully (from cache)", data, meta));
          }

          int page = req.getPage() > 0 ? req.getPage() : 1;
          int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
          String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

          FindAllTransactions findAllReq = FindAllTransactions.builder()
              .search(keyword)
              .page(page)
              .pageSize(pageSize)
              .build();

          return repo.getTrashedTransactions(findAllReq)
              .compose(result -> {
                ApiResponsePagination<List<TransactionResponseDeleteAt>> response = mapTransactionPaginationDeleteAt(
                    result, page, pageSize);
                return redisService.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL).map(v -> response);
              })
              .onSuccess(response -> {
                span.setAttribute("transactions.count", (long) response.data().size());
                span.setAttribute("transactions.total_records", (long) response.pagination().totalRecords());
                tracingMetrics.completeSpanSuccess(tracingContext, "get_trashed",
                    "Trashed transactions fetched successfully");
              });
        })
        .recover(throwable -> {
          logger.error("Failed to fetch trashed transactions", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_trashed", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch trashed transactions: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<TransactionResponse>> getTransactionById(Integer transactionId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransactionQueryService.getTransactionById",
        Attributes.builder()
            .put("transaction.id", (long) transactionId)
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Fetching transaction by id: {}", transactionId);
    String cacheKey = CACHE_PREFIX + transactionId;

    return redisService.get(cacheKey)
        .compose(cachedTransaction -> {
          if (cachedTransaction != null && !cachedTransaction.isEmpty()) {
            logger.info("Transaction {} found in cache", transactionId);
            span.setAttribute("transaction.cache_hit", true);
            try {
              Transaction transaction = Transaction.fromJson(new JsonObject(cachedTransaction));
              tracingMetrics.completeSpanSuccess(tracingContext, "get_by_id", "Transaction fetched from cache");
              return Future.succeededFuture(ApiResponse.success(
                  "Transaction fetched successfully (from cache)",
                  TransactionResponse.from(transaction)));
            } catch (Exception e) {
              logger.warn("Failed to parse cached transaction data for transaction {}: {}", transactionId,
                  e.getMessage());
              return fetchTransactionFromDatabase(transactionId, tracingContext);
            }
          } else {
            span.setAttribute("transaction.cache_hit", false);
            return fetchTransactionFromDatabase(transactionId, tracingContext);
          }
        })
        .recover(err -> {
          logger.error("Failed to fetch transaction by id: {}", transactionId, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_id", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.error("Failed to fetch transaction: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<TransactionResponse>>> getTransactionsByCardNumber(
      FindAllTransactionCardNumberRequest req) {
    String cacheKey = CACHE_PREFIX + "card:" + req.getCardNumber() + ":" + req.getPage() + ":" + req.getPageSize() + ":"
        + req.getSearch();
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("TransactionQueryService.getTransactionsByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("transaction.cache_hit", true);
            JsonObject json = new JsonObject(cached);
            List<TransactionResponse> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionResponse.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card", "Success (from cache)");
            return Future.succeededFuture(new ApiResponsePagination<>("success",
                "Transactions for card fetched successfully (from cache)", data, meta));
          }

          int page = req.getPage() > 0 ? req.getPage() : 1;
          int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
          String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";
          String cardNumber = req.getCardNumber();

          FindAllTransactionCardNumber findAllReq = FindAllTransactionCardNumber.builder()
              .cardNumber(cardNumber)
              .search(keyword)
              .page(page)
              .pageSize(pageSize)
              .build();

          return repo.getTransactionsByCardNumber(findAllReq)
              .compose(result -> {
                ApiResponsePagination<List<TransactionResponse>> response = mapTransactionPagination(result, page,
                    pageSize);
                return redisService.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL).map(v -> response);
              })
              .onSuccess(response -> {
                span.setAttribute("transactions.count", (long) response.data().size());
                span.setAttribute("transactions.total_records", (long) response.pagination().totalRecords());
                tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card",
                    "Transactions for card fetched successfully");
              });
        })
        .recover(throwable -> {
          logger.error("Failed to fetch transactions for card", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_by_card", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch transactions for card: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<List<TransactionResponse>>> getTransactionsByMerchantId(int merchantId) {
    String cacheKey = CACHE_PREFIX + "merchant:" + merchantId;
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("TransactionQueryService.getTransactionsByMerchantId");

    return redisService.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            List<TransactionResponse> data = new JsonArray(cached).stream()
                .map(o -> ((JsonObject) o).mapTo(TransactionResponse.class))
                .toList();
            tracingMetrics.completeSpanSuccess(tracingContext, "get_by_merchant", "Success (from cache)");
            return Future.succeededFuture(ApiResponse.success("Transactions found (from cache)", data));
          }
          return repo.getTransactionsByMerchantId(merchantId)
              .compose(list -> {
                List<TransactionResponse> data = list.getData().stream().map(TransactionResponse::from).toList();
                return redisService.setJson(cacheKey, new JsonArray(data), CACHE_TTL).map(v -> data);
              })
              .map(data -> ApiResponse.success("Transactions found", data))
              .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_by_merchant", "Success"))
              .recover(err -> {
                tracingMetrics.completeSpanError(tracingContext, "get_by_merchant", err.getMessage());
                return Future.succeededFuture(ApiResponse.error(err.getMessage()));
              });
        });
  }

  private Future<ApiResponse<TransactionResponse>> fetchTransactionFromDatabase(Integer transactionId,
      TracingMetrics.TracingContext tracingContext) {
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    return repo.getTransactionById(transactionId)
        .compose((Transaction transaction) -> {
          if (transaction == null) {
            return Future.failedFuture(new NotFoundException("Transaction not found"));
          }

          span.setAttribute("transaction.card_number", Objects.requireNonNull(transaction.getCardNumber()));

          String cacheKey = CACHE_PREFIX + transactionId;
          redisService.setJson(cacheKey, transaction.toJson(), CACHE_TTL)
              .onSuccess(v -> logger.debug("Transaction {} cached successfully", transactionId))
              .onFailure(err -> logger.warn("Failed to cache transaction {}: {}", transactionId, err.getMessage()));

          return Future.succeededFuture(ApiResponse.success(
              "Transaction fetched successfully",
              TransactionResponse.from(transaction)));
        });
  }

  private ApiResponsePagination<List<TransactionResponse>> mapTransactionPagination(
      PagedResult<Transaction> result,
      int page,
      int pageSize) {

    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<TransactionResponse> data = result.getData()
        .stream()
        .map(TransactionResponse::from)
        .toList();

    return new ApiResponsePagination<>(
        "success",
        "Transactions found",
        data,
        new PaginationMeta(
            page,
            pageSize,
            totalPages,
            totalRecords));
  }

  private ApiResponsePagination<List<TransactionResponseDeleteAt>> mapTransactionPaginationDeleteAt(
      PagedResult<Transaction> result,
      int page,
      int pageSize) {

    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<TransactionResponseDeleteAt> data = result.getData()
        .stream()
        .map(TransactionResponseDeleteAt::from)
        .toList();

    return new ApiResponsePagination<>(
        "success",
        "Trashed transactions found",
        data,
        new PaginationMeta(
            page,
            pageSize,
            totalPages,
            totalRecords));
  }
}
