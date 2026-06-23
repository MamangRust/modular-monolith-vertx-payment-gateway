package io.example.withdraw.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.withdraw.domain.requests.FindAllWithdrawCardNumber;
import io.example.withdraw.domain.requests.FindAllWithdraws;
import io.example.withdraw.model.Withdraw;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.example.withdraw.repository.WithdrawQueryRepository;
import io.example.withdraw.service.WithdrawQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WithdrawQueryServiceImpl implements WithdrawQueryService {
  private static final Logger logger = LoggerFactory.getLogger(WithdrawQueryServiceImpl.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final WithdrawQueryRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "withdraw:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  private PagedResult<WithdrawResponse> mapWithdrawPagination(PagedResult<Withdraw> result, int page, int pageSize) {
    int totalRecords = result.getTotalRecords();
    List<WithdrawResponse> data = result.getData().stream().map(WithdrawResponse::from).toList();
    return new PagedResult<>(data, totalRecords);
  }

  private PagedResult<WithdrawResponseDeleteAt> mapWithdrawPaginationDeleteAt(PagedResult<Withdraw> result, int page,
      int pageSize) {
    int totalRecords = result.getTotalRecords();
    List<WithdrawResponseDeleteAt> data = result.getData().stream().map(WithdrawResponseDeleteAt::from).toList();
    return new PagedResult<>(data, totalRecords);
  }

  @Override
  public Future<PagedResult<WithdrawResponse>> getWithdraws(FindAllWithdraws req) {
    var tracingContext = tracingMetrics.startSpan("WithdrawQueryService.getWithdraws");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    String cacheKey = CACHE_PREFIX + "list:" + req.getSearch() + ":" + req.getPage() + ":" + req.getPageSize();

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              PagedResult<Withdraw> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Withdraw>>() {
              });
              return Future.succeededFuture(mapWithdrawPagination(typedCached, req.getPage(), req.getPageSize()));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached withdrawals: {}", e.getMessage());
            }
          }
          span.setAttribute("cache.hit", false);
          return repo.getWithdraws(req)
              .compose(result -> redisService.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(result -> mapWithdrawPagination(result, req.getPage(), req.getPageSize()));
        })
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_all", "Withdrawals fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch withdrawals", err);
          tracingMetrics.completeSpanError(tracingContext, "get_all", err.getMessage());
        });
  }

  @Override
  public Future<PagedResult<WithdrawResponse>> getWithdrawsByCardNumber(FindAllWithdrawCardNumber req) {
    var tracingContext = tracingMetrics.startSpan("WithdrawQueryService.getWithdrawsByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    String cacheKey = CACHE_PREFIX + "card:" + req.getCardNumber() + ":" + req.getSearch() + ":" + req.getPage() + ":"
        + req.getPageSize();

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              PagedResult<Withdraw> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Withdraw>>() {
              });
              return Future.succeededFuture(mapWithdrawPagination(typedCached, req.getPage(), req.getPageSize()));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached withdrawals by card number: {}", e.getMessage());
            }
          }
          span.setAttribute("cache.hit", false);
          return repo.getWithdrawsByCardNumber(req.getCardNumber(), req.getSearch(), req.getPage(), req.getPageSize())
              .compose(result -> redisService.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(result -> mapWithdrawPagination(result, req.getPage(), req.getPageSize()));
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card",
            "Withdrawals for card fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch withdrawals for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_card", err.getMessage());
        });
  }

  // TODO: cache
  @Override
  public Future<PagedResult<WithdrawResponseDeleteAt>> getActiveWithdraws(FindAllWithdraws req) {
    var tracingContext = tracingMetrics.startSpan("WithdrawQueryService.getActiveWithdraws");

    return repo.getActiveWithdraws(req)
        .map(result -> mapWithdrawPaginationDeleteAt(result, req.getPage(), req.getPageSize()))
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_active",
            "Active withdrawals fetched successfully"))
        .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, "get_active", err.getMessage()));
  }

  // TODO: cache
  @Override
  public Future<PagedResult<WithdrawResponseDeleteAt>> getTrashedWithdraws(FindAllWithdraws req) {
    var tracingContext = tracingMetrics.startSpan("WithdrawQueryService.getTrashedWithdraws");

    return repo.getTrashedWithdraws(req)
        .map(result -> mapWithdrawPaginationDeleteAt(result, req.getPage(), req.getPageSize()))
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_trashed",
            "Trashed withdrawals fetched successfully"))
        .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, "get_trashed", err.getMessage()));
  }

  @Override
  public Future<WithdrawResponse> getWithdrawById(Integer withdrawId) {
    var tracingContext = tracingMetrics.startSpan("WithdrawQueryService.getWithdrawById",
        Attributes.builder().put("withdraw.id", (long) withdrawId).build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    String cacheKey = CACHE_PREFIX + withdrawId;

    return redisService.getJson(cacheKey, Withdraw.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            return Future.succeededFuture(WithdrawResponse.from(cached));
          }
          span.setAttribute("cache.hit", false);
          return repo.getWithdrawById(withdrawId)
              .compose(withdraw -> {
                if (withdraw == null) {
                  return Future.<Withdraw>failedFuture(new NotFoundException("Withdrawal not found"));
                }
                return redisService.setJson(cacheKey, withdraw, CACHE_TTL).<Withdraw>map(v -> withdraw);
              })
              .map(WithdrawResponse::from);
        })
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_by_id", "Withdraw fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch withdraw by id: {}", withdrawId, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_id", err.getMessage());
        });
  }

  @Override
  public Future<List<WithdrawResponse>> getWithdrawsByCardNumberPrimitive(String cardNumber) {
    var tracingContext = tracingMetrics.startSpan("WithdrawQueryService.getWithdrawsByCardNumberPrimitive",
        Attributes.builder().put("card.number", Objects.requireNonNull(cardNumber)).build());

    String cacheKey = CACHE_PREFIX + "card_primitive:" + cardNumber;

    return redisService.getJsonList(cacheKey, Withdraw.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            return Future.succeededFuture(cached.stream().map(WithdrawResponse::from).toList());
          }
          return repo.getWithdrawsByCardNumberPrimitive(cardNumber)
              .compose(list -> {
                if (list == null || list.isEmpty()) {
                  return Future.succeededFuture(List.<Withdraw>of());
                }
                return redisService.setJsonList(cacheKey, list, CACHE_TTL).<List<Withdraw>>map(v -> list);
              })
              .map(list -> list.stream().map(WithdrawResponse::from).toList());
        })
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card_primitive", "Withdrawals found"))
        .onFailure(err -> {
          logger.error("Failed to fetch withdrawals for card primitive: {}", cardNumber, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_card_primitive", err.getMessage());
        });
  }
}