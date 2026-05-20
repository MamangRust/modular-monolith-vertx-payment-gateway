package io.example.transfer.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.common.model.PagedResult;
import io.example.common.model.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transfer.model.Transfer;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.example.transfer.repository.TransferQueryRepository;
import io.example.transfer.service.TransferQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.example.transfer.domain.requests.FindAllTransfers;
import pb.transfer.Transfer.FindAllTransferRequest;

public class TransferQueryServiceImpl implements TransferQueryService {
  private static final Logger logger = LoggerFactory.getLogger(TransferQueryServiceImpl.class);

  private final TransferQueryRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "transfer:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public TransferQueryServiceImpl(
      TransferQueryRepository repo,
      RedisService redisService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.redisService = redisService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponsePagination<List<TransferResponse>>> getAllTransfers(
      FindAllTransferRequest req) {

    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("TransferService.getAllTransfers");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    FindAllTransfers domainReq = FindAllTransfers.builder()
        .page(req.getPage() > 0 ? req.getPage() : 1)
        .pageSize(req.getPageSize() > 0 ? req.getPageSize() : 10)
        .search(req.getSearch() != null ? req.getSearch() : "")
        .build();

    logger.info(
        "Fetching transfers | search={}, page={}, pageSize={}",
        domainReq.getSearch(), domainReq.getPage(), domainReq.getPageSize());

    return repo.getTransfers(domainReq)
        .map(result -> {
          ApiResponsePagination<List<TransferResponse>> response = mapTransferPagination(result, domainReq);
          span.setAttribute("transfers.count", response.data().size());
          span.setAttribute("transfers.total_records", response.pagination().totalRecords());
          tracingMetrics.completeSpanSuccess(tracingContext, "get_all", "Transfers fetched successfully");
          return response;
        })
        .recover(throwable -> {
          logger.error("Failed to fetch transfers", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_all", throwable.getMessage());

          return Future.succeededFuture(
              ApiResponsePagination
                  .<List<TransferResponse>>error("Failed to fetch transfers: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<TransferResponseDeleteAt>>> getActiveTransfers(
      FindAllTransferRequest req) {

    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("TransferService.getActiveTransfers");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    FindAllTransfers domainReq = FindAllTransfers.builder()
        .page(req.getPage() > 0 ? req.getPage() : 1)
        .pageSize(req.getPageSize() > 0 ? req.getPageSize() : 10)
        .search(req.getSearch() != null ? req.getSearch() : "")
        .build();

    logger.info(
        "Fetching active transfers | search={}, page={}, pageSize={}",
        domainReq.getSearch(), domainReq.getPage(), domainReq.getPageSize());

    return repo.getActiveTransfers(domainReq)
        .map(result -> {
          ApiResponsePagination<List<TransferResponseDeleteAt>> response = mapTransferPaginationDeleteAt(result,
              domainReq);
          span.setAttribute("transfer.count", response.data().size());
          span.setAttribute("transfer.total_records", response.pagination().totalRecords());

          tracingMetrics.completeSpanSuccess(tracingContext, "get_active", "Active transfers fetched successfully");
          return response;
        })
        .recover(throwable -> {
          logger.error("Failed to fetch active transfers", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_active", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination
                  .<List<TransferResponseDeleteAt>>error(
                      "Failed to fetch active transfers: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<TransferResponseDeleteAt>>> getTrashedTransfers(
      FindAllTransferRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("TransferService.getTrashedTransfers");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    FindAllTransfers domainReq = FindAllTransfers.builder()
        .page(req.getPage() > 0 ? req.getPage() : 1)
        .pageSize(req.getPageSize() > 0 ? req.getPageSize() : 10)
        .search(req.getSearch() != null ? req.getSearch() : "")
        .build();

    logger.info(
        "Fetching trashed transfers | search={}, page={}, pageSize={}",
        domainReq.getSearch(), domainReq.getPage(), domainReq.getPageSize());

    return repo.getTrashedTransfers(domainReq)
        .map(result -> {
          ApiResponsePagination<List<TransferResponseDeleteAt>> response = mapTransferPaginationDeleteAt(result,
              domainReq);
          span.setAttribute("transfer.count", response.data().size());
          span.setAttribute("transfer.total_records", response.pagination().totalRecords());
          tracingMetrics.completeSpanSuccess(tracingContext, "get_trashed", "Trashed transfers fetched successfully");
          return response;
        })
        .recover(throwable -> {
          logger.error("Failed to fetch trashed transfers", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_trashed", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination
                  .<List<TransferResponseDeleteAt>>error(
                      "Failed to fetch trashed transfers: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<TransferResponse>> getTransferById(Integer transferId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransferService.getTransferById",
        Attributes.builder()
            .put("transfer.id", transferId)
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Fetching transfer by id: {}", transferId);
    String cacheKey = "transfer:" + transferId;

    return redisService.get(cacheKey)
        .compose(cachedTransfer -> {
          if (cachedTransfer != null && !cachedTransfer.isEmpty()) {
            logger.info("Transfer {} found in cache", transferId);
            span.setAttribute("transfer.cache_hit", true);
            try {
              Transfer transfer = Transfer.fromJson(new JsonObject(cachedTransfer));
              tracingMetrics.completeSpanSuccess(tracingContext, "get_by_id", "Transfer fetched from cache");
              return Future.succeededFuture(ApiResponse.success(
                  "Transfer fetched successfully (from cache)",
                  TransferResponse.from(transfer)));
            } catch (Exception e) {
              logger.warn("Failed to parse cached transfer data for transfer {}: {}", transferId, e.getMessage());
              return fetchTransferFromDatabase(transferId, tracingContext);
            }
          } else {
            span.setAttribute("transfer.cache_hit", false);
            return fetchTransferFromDatabase(transferId, tracingContext);
          }
        })
        .recover(err -> {
          logger.error("Failed to fetch transfer by id: {}", transferId, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_id", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<TransferResponse>error(
                  "Failed to fetch transfer: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<List<TransferResponse>>> getTransfersByCardNumber(String cardNumber) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransferService.getTransfersByCardNumber",
        Attributes.builder()
            .put("transfer.card_number", Objects.requireNonNull(cardNumber))
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Fetching all transfers for card number: {}", cardNumber);

    return repo.getTransfersByCardNumber(cardNumber)
        .map(transfers -> {
          List<TransferResponse> responseList = transfers.stream()
              .map(TransferResponse::from)
              .toList();
          span.setAttribute("transfers.count", responseList.size());
          tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card", "Transfers for card fetched successfully");
          return ApiResponse.success("Transfers for card fetched successfully", responseList);
        })
        .recover(err -> {
          logger.error("Failed to fetch transfers for card number: {}", cardNumber, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_card", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<List<TransferResponse>>error("Failed to fetch transfers for card: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<List<TransferResponse>>> getTransfersAsSender(String cardNumber) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransferService.getTransfersAsSender",
        Attributes.builder()
            .put("transfer.from_card", Objects.requireNonNull(cardNumber))
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Fetching transfers sent from card number: {}", cardNumber);

    return repo.getTransfersBySender(cardNumber)
        .map(transfers -> {
          List<TransferResponse> responseList = transfers.stream()
              .map(TransferResponse::from)
              .toList();
          span.setAttribute("transfers.count", responseList.size());
          tracingMetrics.completeSpanSuccess(tracingContext, "get_as_sender", "Sent transfers fetched successfully");
          return ApiResponse.success("Sent transfers fetched successfully", responseList);
        })
        .recover(err -> {
          logger.error("Failed to fetch sent transfers for card number: {}", cardNumber, err);
          tracingMetrics.completeSpanError(tracingContext, "get_as_sender", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<List<TransferResponse>>error("Failed to fetch sent transfers: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<List<TransferResponse>>> getTransfersAsReceiver(String cardNumber) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransferService.getTransfersAsReceiver",
        Attributes.builder()
            .put("transfer.to_card", Objects.requireNonNull(cardNumber))
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Fetching transfers received by card number: {}", cardNumber);

    return repo.getTransfersByReceiver(cardNumber)
        .map(transfers -> {
          List<TransferResponse> responseList = transfers.stream()
              .map(TransferResponse::from)
              .toList();
          span.setAttribute("transfers.count", responseList.size());
          tracingMetrics.completeSpanSuccess(tracingContext, "get_as_receiver",
              "Received transfers fetched successfully");
          return ApiResponse.success("Received transfers fetched successfully", responseList);
        })
        .recover(err -> {
          logger.error("Failed to fetch received transfers for card number: {}", cardNumber, err);
          tracingMetrics.completeSpanError(tracingContext, "get_as_receiver", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<List<TransferResponse>>error("Failed to fetch received transfers: " + err.getMessage()));
        });
  }

  private Future<ApiResponse<TransferResponse>> fetchTransferFromDatabase(Integer transferId,
      TracingMetrics.TracingContext tracingContext) {
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    return repo.getTransferById(transferId)
        .compose((Transfer transfer) -> {
          if (transfer == null) {
            return Future.failedFuture(new NotFoundException("Transfer not found"));
          }

          span.setAttribute("transfer.from", Objects.requireNonNull(transfer.getTransferFrom()));
          span.setAttribute("transfer.to", Objects.requireNonNull(transfer.getTransferTo()));

          String cacheKey = CACHE_PREFIX + transferId;
          redisService.setJson(cacheKey, transfer.toJson(), CACHE_TTL)
              .onSuccess(v -> logger.debug("Transfer {} cached successfully", transferId))
              .onFailure(err -> logger.warn("Failed to cache transfer {}: {}", transferId, err.getMessage()));

          return Future.succeededFuture(ApiResponse.success(
              "Transfer fetched successfully",
              TransferResponse.from(transfer)));
        });
  }

  private ApiResponsePagination<List<TransferResponse>> mapTransferPagination(
      PagedResult<Transfer> result,
      FindAllTransfers req) {

    int pageSize = req.getPageSize();
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<TransferResponse> data = result.getData()
        .stream()
        .map(TransferResponse::from)
        .toList();

    return new ApiResponsePagination<>(
        "success",
        "Transfers found",
        data,
        new PaginationMeta(
            req.getPage(),
            pageSize,
            totalPages,
            totalRecords));
  }

  private ApiResponsePagination<List<TransferResponseDeleteAt>> mapTransferPaginationDeleteAt(
      PagedResult<Transfer> result,
      FindAllTransfers req) {

    int pageSize = req.getPageSize();
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<TransferResponseDeleteAt> data = result.getData()
        .stream()
        .map(TransferResponseDeleteAt::from)
        .toList();

    return new ApiResponsePagination<>(
        "success",
        "Transfers found",
        data,
        new PaginationMeta(
            req.getPage(),
            pageSize,
            totalPages,
            totalRecords));
  }
}
