package io.example.card.service.impl;

import java.time.Duration;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.card.model.Card;
import io.example.card.model.CardEmail;
import io.example.card.repository.CardQueryRepository;
import io.example.card.service.CardQueryService;
import io.example.common.domain.PagedResult;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.Card.FindAllCardRequest;

@RequiredArgsConstructor
public class CardQueryServiceImpl implements CardQueryService {
  private static final Logger logger = LoggerFactory.getLogger(CardQueryServiceImpl.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final CardQueryRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);
  private static final String CACHE_PREFIX = "card:";

  private int safePage(int page) {
    return page > 0 ? page : 1;
  }

  private int safePageSize(int size) {
    return size > 0 ? size : 10;
  }

  @Override
  public Future<PagedResult<Card>> getCards(FindAllCardRequest request) {
    var ctx = metrics.startSpan("CardQueryService.getCards");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(request.getPage());
    int pageSize = safePageSize(request.getPageSize());
    String cacheKey = String.format("%sall:p:%d:s:%d", CACHE_PREFIX, page, pageSize);

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card.cache_hit", true);
              PagedResult<Card> cached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Card>>() {
              });
              return Future.succeededFuture(cached);
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached cards: {}", e.getMessage());
            }
          }
          span.setAttribute("card.cache_hit", false);
          return repository.findAllCards(request)
              .compose(result -> redis.setJson(cacheKey, result, CACHE_TTL).map(v -> result));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCards", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getCards", e.getMessage()));
  }

  @Override
  public Future<PagedResult<Card>> getActiveCards(FindAllCardRequest request) {
    var ctx = metrics.startSpan("CardQueryService.getActiveCards");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(request.getPage());
    int pageSize = safePageSize(request.getPageSize());
    String cacheKey = String.format("%sactive:p:%d:s:%d", CACHE_PREFIX, page, pageSize);

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card.cache_hit", true);
              PagedResult<Card> cached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Card>>() {
              });
              return Future.succeededFuture(cached);
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached active cards: {}", e.getMessage());
            }
          }
          span.setAttribute("card.cache_hit", false);
          return repository.findByActive(request)
              .compose(result -> redis.setJson(cacheKey, result, CACHE_TTL).map(v -> result));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getActiveCards", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getActiveCards", e.getMessage()));
  }

  @Override
  public Future<PagedResult<Card>> getTrashedCards(FindAllCardRequest request) {
    var ctx = metrics.startSpan("CardQueryService.getTrashedCards");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(request.getPage());
    int pageSize = safePageSize(request.getPageSize());
    String cacheKey = String.format("%strashed:p:%d:s:%d", CACHE_PREFIX, page, pageSize);

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card.cache_hit", true);
              PagedResult<Card> cached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Card>>() {
              });
              return Future.succeededFuture(cached);
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached trashed cards: {}", e.getMessage());
            }
          }
          span.setAttribute("card.cache_hit", false);
          return repository.findByTrashed(request)
              .compose(result -> redis.setJson(cacheKey, result, CACHE_TTL).map(v -> result));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTrashedCards", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getTrashedCards", e.getMessage()));
  }

  @Override
  public Future<Card> getCardById(Integer cardId) {
    var ctx = metrics.startSpan("CardQueryService.getCardById");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "id:" + cardId;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card.cache_hit", true);
              Card card = mapper.readValue(jsonStr, Card.class);
              return Future.succeededFuture(card);
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached card: {}", e.getMessage());
            }
          }
          span.setAttribute("card.cache_hit", false);
          return repository.findById(cardId)
              .compose(card -> {
                if (card == null)
                  return Future.failedFuture(new NotFoundException("Card not found"));
                return redis.setJson(cacheKey, card, CACHE_TTL).map(v -> card);
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCardById", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getCardById", e.getMessage()));
  }

  @Override
  public Future<Card> getCardByUserId(Integer userId) {
    var ctx = metrics.startSpan("CardQueryService.getCardByUserId");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "user:" + userId;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card.cache_hit", true);
              Card card = mapper.readValue(jsonStr, Card.class);
              return Future.succeededFuture(card);
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached card by userId: {}", e.getMessage());
            }
          }
          span.setAttribute("card.cache_hit", false);
          return repository.findByUserId(userId)
              .compose(card -> {
                if (card == null)
                  return Future.failedFuture(new NotFoundException("Card not found"));
                return redis.setJson(cacheKey, card, CACHE_TTL).map(v -> card);
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCardByUserId", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getCardByUserId", e.getMessage()));
  }

  @Override
  public Future<Card> getCardByCardNumber(String cardNumber) {
    var ctx = metrics.startSpan("CardQueryService.getCardByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "number:" + cardNumber;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card.cache_hit", true);
              Card card = mapper.readValue(jsonStr, Card.class);
              return Future.succeededFuture(card);
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached card by cardNumber: {}", e.getMessage());
            }
          }
          span.setAttribute("card.cache_hit", false);
          return repository.findByCardNumber(cardNumber)
              .compose(card -> {
                if (card == null)
                  return Future.failedFuture(new NotFoundException("Card not found"));
                return redis.setJson(cacheKey, card, CACHE_TTL).map(v -> card);
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCardByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getCardByCardNumber", e.getMessage()));
  }

  @Override
  public Future<CardEmail> getCardEmailByCardNumber(String cardNumber) {
    return repository.getCardEmailByCardNumber(cardNumber)
        .compose(card -> {
          if (card == null)
            return Future.failedFuture(new NotFoundException("Card not found"));

          return Future.succeededFuture(CardEmail.builder()
              .id(card.getId())
              .email(card.getEmail())
              .userId(card.getUserId())
              .cardNumber(card.getCardNumber())
              .cardType(card.getCardType())
              .expireDate(card.getExpireDate())
              .cvv(card.getCvv())
              .cardProvider(card.getCardProvider())
              .createdAt(card.getCreatedAt())
              .updatedAt(card.getUpdatedAt())
              .build());
        });
  }
}