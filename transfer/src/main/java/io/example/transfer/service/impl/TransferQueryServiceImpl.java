package io.example.transfer.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transfer.domain.requests.FindAllTransfers;
import io.example.transfer.model.Transfer;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.example.transfer.repository.TransferQueryRepository;
import io.example.transfer.service.TransferQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransferQueryServiceImpl implements TransferQueryService {
  private static final Logger logger = LoggerFactory.getLogger(TransferQueryServiceImpl.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final TransferQueryRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "transfer:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  private PagedResult<TransferResponse> mapTransferPagination(PagedResult<Transfer> result, int page, int pageSize) {
    int totalRecords = result.getTotalRecords();
    List<TransferResponse> data = result.getData().stream().map(TransferResponse::from).toList();
    return new PagedResult<>(data, totalRecords);
  }

  private PagedResult<TransferResponseDeleteAt> mapTransferPaginationDeleteAt(PagedResult<Transfer> result, int page,
      int pageSize) {
    int totalRecords = result.getTotalRecords();
    List<TransferResponseDeleteAt> data = result.getData().stream().map(TransferResponseDeleteAt::from).toList();
    return new PagedResult<>(data, totalRecords);
  }

  @Override
  public Future<PagedResult<TransferResponse>> getAllTransfers(FindAllTransfers req) {
    var tracingContext = tracingMetrics.startSpan("TransferQueryService.getAllTransfers");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    String cacheKey = CACHE_PREFIX + "list:" + req.getSearch() + ":" + req.getPage() + ":" + req.getPageSize();

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              PagedResult<Transfer> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Transfer>>() {
              });
              return Future.succeededFuture(mapTransferPagination(typedCached, req.getPage(), req.getPageSize()));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached transfers: {}", e.getMessage());
            }
          }
          span.setAttribute("cache.hit", false);
          return repo.getTransfers(req)
              .compose(result -> redisService.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(result -> mapTransferPagination(result, req.getPage(), req.getPageSize()));
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_all", "Transfers fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch transfers", err);
          tracingMetrics.completeSpanError(tracingContext, "get_all", err.getMessage());
        });
  }

  @Override
  public Future<PagedResult<TransferResponseDeleteAt>> getActiveTransfers(FindAllTransfers req) {
    var tracingContext = tracingMetrics.startSpan("TransferQueryService.getActiveTransfers");

    return repo.getActiveTransfers(req)
        .map(result -> mapTransferPaginationDeleteAt(result, req.getPage(), req.getPageSize()))
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_active",
            "Active transfers fetched successfully"))
        .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, "get_active", err.getMessage()));
  }

  @Override
  public Future<PagedResult<TransferResponseDeleteAt>> getTrashedTransfers(FindAllTransfers req) {
    var tracingContext = tracingMetrics.startSpan("TransferQueryService.getTrashedTransfers");

    return repo.getTrashedTransfers(req)
        .map(result -> mapTransferPaginationDeleteAt(result, req.getPage(), req.getPageSize()))
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_trashed",
            "Trashed transfers fetched successfully"))
        .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, "get_trashed", err.getMessage()));
  }

  @Override
  public Future<TransferResponse> getTransferById(Integer transferId) {
    var tracingContext = tracingMetrics.startSpan("TransferQueryService.getTransferById",
        Attributes.builder().put("transfer.id", (long) transferId).build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    String cacheKey = CACHE_PREFIX + transferId;

    return redisService.getJson(cacheKey, Transfer.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            return Future.succeededFuture(TransferResponse.from(cached));
          }
          span.setAttribute("cache.hit", false);
          return repo.getTransferById(transferId)
              .compose(transfer -> {
                if (transfer == null) {
                  return Future.<Transfer>failedFuture(new NotFoundException("Transfer not found"));
                }
                return redisService.setJson(cacheKey, transfer, CACHE_TTL).<Transfer>map(v -> transfer);
              })
              .map(TransferResponse::from);
        })
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_by_id", "Transfer fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch transfer by id: {}", transferId, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_id", err.getMessage());
        });
  }

  @Override
  public Future<List<TransferResponse>> getTransfersByCardNumber(String cardNumber) {
    var tracingContext = tracingMetrics.startSpan("TransferQueryService.getTransfersByCardNumber",
        Attributes.builder().put("transfer.card_number", Objects.requireNonNull(cardNumber)).build());

    String cacheKey = CACHE_PREFIX + "card_primitive:" + cardNumber;

    return redisService.getJsonList(cacheKey, Transfer.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached.stream().map(TransferResponse::from).toList());
          }
          return repo.getTransfersByCardNumber(cardNumber)
              .compose(list -> {
                if (list == null || list.isEmpty()) {
                  return Future.succeededFuture(List.<Transfer>of());
                }
                return redisService.setJsonList(cacheKey, list, CACHE_TTL).<List<Transfer>>map(v -> list);
              })
              .map(list -> list.stream().map(TransferResponse::from).toList());
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card",
            "Transfers for card fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch transfers for card number: {}", cardNumber, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_card", err.getMessage());
        });
  }

  @Override
  public Future<List<TransferResponse>> getTransfersAsSender(String cardNumber) {
    var tracingContext = tracingMetrics.startSpan("TransferQueryService.getTransfersAsSender",
        Attributes.builder().put("transfer.from_card", Objects.requireNonNull(cardNumber)).build());

    String cacheKey = CACHE_PREFIX + "sender:" + cardNumber;

    return redisService.getJsonList(cacheKey, Transfer.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached.stream().map(TransferResponse::from).toList());
          }
          return repo.getTransfersBySender(cardNumber)
              .compose(list -> {
                if (list == null || list.isEmpty()) {
                  return Future.succeededFuture(List.<Transfer>of());
                }
                return redisService.setJsonList(cacheKey, list, CACHE_TTL).<List<Transfer>>map(v -> list);
              })
              .map(list -> list.stream().map(TransferResponse::from).toList());
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_as_sender",
            "Sent transfers fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch sent transfers for card number: {}", cardNumber, err);
          tracingMetrics.completeSpanError(tracingContext, "get_as_sender", err.getMessage());
        });
  }

  @Override
  public Future<List<TransferResponse>> getTransfersAsReceiver(String cardNumber) {
    var tracingContext = tracingMetrics.startSpan("TransferQueryService.getTransfersAsReceiver",
        Attributes.builder().put("transfer.to_card", Objects.requireNonNull(cardNumber)).build());

    String cacheKey = CACHE_PREFIX + "receiver:" + cardNumber;

    return redisService.getJsonList(cacheKey, Transfer.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached.stream().map(TransferResponse::from).toList());
          }
          return repo.getTransfersByReceiver(cardNumber)
              .compose(list -> {
                if (list == null || list.isEmpty()) {
                  return Future.succeededFuture(List.<Transfer>of());
                }
                return redisService.setJsonList(cacheKey, list, CACHE_TTL).<List<Transfer>>map(v -> list);
              })
              .map(list -> list.stream().map(TransferResponse::from).toList());
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_as_receiver",
            "Received transfers fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch received transfers for card number: {}", cardNumber, err);
          tracingMetrics.completeSpanError(tracingContext, "get_as_receiver", err.getMessage());
        });
  }
}