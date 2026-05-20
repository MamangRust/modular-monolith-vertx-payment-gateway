package io.example.card.service.impl;

import java.util.List;
import io.example.card.model.Card;
import io.example.card.model.CardEmail;
import io.example.card.repository.CardQueryRepository;
import io.example.card.service.CardQueryService;
import io.example.common.domain.ApiResponse;
import io.example.common.domain.ApiResponsePagination;
import io.example.common.domain.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import pb.card.Card.FindAllCardRequest;
import pb.card.Card.FindByIdCardRequest;
import pb.card.Card.FindByUserIdCardRequest;
import pb.card.Card.FindByCardNumberRequest;
import java.time.Duration;

public class CardQueryServiceImpl implements CardQueryService {
  private final CardQueryRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public CardQueryServiceImpl(CardQueryRepository repository, RedisService redis, TracingMetrics metrics) {
    this.repository = repository;
    this.redis = redis;
    this.metrics = metrics;
  }

  private PaginationMeta calculateMeta(int total, int page, int size) {
    int totalPages = (int) Math.ceil((double) total / size);
    return new PaginationMeta(page, size, totalPages, total);
  }

  @Override
  public Future<ApiResponsePagination<List<Card>>> getCards(FindAllCardRequest request) {
    var ctx = metrics.startSpan("CardQueryService.getCards");
    return repository.findAllCards(request)
        .map(res -> ApiResponsePagination.success(
            "Cards retrieved successfully",
            res.getData(),
            calculateMeta(res.getTotalRecords(), request.getPage(), request.getPageSize())
        ))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCards", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getCards", e.getMessage()));
  }

  @Override
  public Future<ApiResponsePagination<List<Card>>> getActiveCards(FindAllCardRequest request) {
    var ctx = metrics.startSpan("CardQueryService.getActiveCards");
    return repository.findByActive(request)
        .map(res -> ApiResponsePagination.success(
            "Active cards retrieved successfully",
            res.getData(),
            calculateMeta(res.getTotalRecords(), request.getPage(), request.getPageSize())
        ))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getActiveCards", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getActiveCards", e.getMessage()));
  }

  @Override
  public Future<ApiResponsePagination<List<Card>>> getTrashedCards(FindAllCardRequest request) {
    var ctx = metrics.startSpan("CardQueryService.getTrashedCards");
    return repository.findByTrashed(request)
        .map(res -> ApiResponsePagination.success(
            "Trashed cards retrieved successfully",
            res.getData(),
            calculateMeta(res.getTotalRecords(), request.getPage(), request.getPageSize())
        ))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTrashedCards", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getTrashedCards", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Card>> getCardById(FindByIdCardRequest request) {
    var ctx = metrics.startSpan("CardQueryService.getCardById");
    String cacheKey = "card:id:" + request.getCardId();

    return redis.getJson(cacheKey, Card.class)
        .compose(cachedCard -> {
          if (cachedCard != null) {
            return Future.succeededFuture(cachedCard);
          }
          return repository.findById(request.getCardId())
              .compose(card -> {
                if (card != null) {
                  return redis.setJson(cacheKey, card, CACHE_TTL).map(v -> card);
                }
                return Future.succeededFuture(null);
              });
        })
        .map(card -> ApiResponse.success("Card retrieved successfully", card))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCardById", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getCardById", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Card>> getCardByUserId(FindByUserIdCardRequest request) {
    var ctx = metrics.startSpan("CardQueryService.getCardByUserId");
    String cacheKey = "card:user:" + request.getUserId();

    return redis.getJson(cacheKey, Card.class)
        .compose(cachedCard -> {
          if (cachedCard != null) {
            return Future.succeededFuture(cachedCard);
          }
          return repository.findByUserId(request.getUserId())
              .compose(card -> {
                if (card != null) {
                  return redis.setJson(cacheKey, card, CACHE_TTL).map(v -> card);
                }
                return Future.succeededFuture(null);
              });
        })
        .map(card -> ApiResponse.success("Card retrieved successfully", card))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCardByUserId", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getCardByUserId", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Card>> getCardByCardNumber(FindByCardNumberRequest request) {
    var ctx = metrics.startSpan("CardQueryService.getCardByCardNumber");
    String cacheKey = "card:number:" + request.getCardNumber();

    return redis.getJson(cacheKey, Card.class)
        .compose(cachedCard -> {
          if (cachedCard != null) {
            return Future.succeededFuture(cachedCard);
          }
          return repository.findByCardNumber(request.getCardNumber())
              .compose(card -> {
                if (card != null) {
                  return redis.setJson(cacheKey, card, CACHE_TTL).map(v -> card);
                }
                return Future.succeededFuture(null);
              });
        })
        .map(card -> ApiResponse.success("Card retrieved successfully", card))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCardByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getCardByCardNumber", e.getMessage()));
  }

  @Override
  public Future<CardEmail> getCardEmailByCardNumber(String cardNumber) {
    return repository.findByCardNumber(cardNumber).compose(card -> {
        if (card == null) return Future.failedFuture("Card not found");
        // Internal usage usually doesn't need cache here unless specified
        return Future.succeededFuture(null); 
    });
  }
}
