package io.example.topup.service.impl;

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
import io.example.topup.domain.requests.topup.FindAllTopups;
import io.example.topup.domain.requests.topup.FindAllTopupsByCardNumber;
import io.example.topup.model.Topup;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.example.topup.repository.TopupQueryRepository;
import io.example.topup.service.TopupQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TopupQueryServiceImpl implements TopupQueryService {
  private static final Logger logger = LoggerFactory.getLogger(TopupQueryServiceImpl.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final TopupQueryRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "topup:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  private PagedResult<TopupResponse> mapTopupPagination(PagedResult<Topup> result, int page, int pageSize) {
    int totalRecords = result.getTotalRecords();
    List<TopupResponse> data = result.getData().stream().map(TopupResponse::from).toList();
    return new PagedResult<>(data, totalRecords);
  }

  private PagedResult<TopupResponseDeleteAt> mapTopupPaginationDeleteAt(PagedResult<Topup> result, int page,
      int pageSize) {
    int totalRecords = result.getTotalRecords();
    List<TopupResponseDeleteAt> data = result.getData().stream().map(TopupResponseDeleteAt::from).toList();
    return new PagedResult<>(data, totalRecords);
  }

  @Override
  public Future<PagedResult<TopupResponse>> getTopups(FindAllTopups req) {
    var tracingContext = tracingMetrics.startSpan("TopupQueryService.getTopups");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    String cacheKey = CACHE_PREFIX + "list:" + req.getSearch() + ":" + req.getPage() + ":" + req.getPageSize();

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              PagedResult<Topup> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Topup>>() {
              });
              return Future.succeededFuture(mapTopupPagination(typedCached, req.getPage(), req.getPageSize()));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached topups: {}", e.getMessage());
            }
          }
          span.setAttribute("cache.hit", false);
          return repo.getTopups(req)
              .compose(result -> redisService.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(result -> mapTopupPagination(result, req.getPage(), req.getPageSize()));
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_all", "Topups fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch topups", err);
          tracingMetrics.completeSpanError(tracingContext, "get_all", err.getMessage());
        });
  }

  @Override
  public Future<PagedResult<TopupResponse>> getTopupsByCardNumber(FindAllTopupsByCardNumber req) {
    var tracingContext = tracingMetrics.startSpan("TopupQueryService.getTopupsByCardNumber");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    String cacheKey = CACHE_PREFIX + "card:" + req.getCardNumber() + ":" + req.getSearch() + ":" + req.getPage() + ":"
        + req.getPageSize();

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("cache.hit", true);
              PagedResult<Topup> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Topup>>() {
              });
              return Future.<PagedResult<TopupResponse>>succeededFuture(
                  mapTopupPagination(typedCached, req.getPage(), req.getPageSize()));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached topups by card: {}", e.getMessage());
            }
          }
          span.setAttribute("cache.hit", false);
          Future<PagedResult<TopupResponse>> fromDb = repo
              .getTopupsByCardNumber(req)
              .compose(result -> redisService.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(result -> mapTopupPagination(result, req.getPage(), req.getPageSize()));

          return fromDb;
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card",
            "Topups by card fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch topups for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_card", err.getMessage());
        });
  }

  @Override
  public Future<PagedResult<TopupResponseDeleteAt>> getActiveTopups(FindAllTopups req) {
    var tracingContext = tracingMetrics.startSpan("TopupQueryService.getActiveTopups");

    return repo.getActiveTopups(req)
        .map(result -> mapTopupPaginationDeleteAt(result, req.getPage(), req.getPageSize()))
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_active", "Active topups fetched successfully"))
        .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, "get_active", err.getMessage()));
  }

  @Override
  public Future<PagedResult<TopupResponseDeleteAt>> getTrashedTopups(FindAllTopups req) {
    var tracingContext = tracingMetrics.startSpan("TopupQueryService.getTrashedTopups");

    return repo.getTrashedTopups(req)
        .map(result -> mapTopupPaginationDeleteAt(result, req.getPage(), req.getPageSize()))
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_trashed",
            "Trashed topups fetched successfully"))
        .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, "get_trashed", err.getMessage()));
  }

  @Override
  public Future<TopupResponse> getTopupById(Integer topupId) {
    var tracingContext = tracingMetrics.startSpan("TopupQueryService.getTopupById",
        Attributes.builder().put("topup.id", (long) topupId).build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    String cacheKey = CACHE_PREFIX + topupId;

    return redisService.getJson(cacheKey, Topup.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("cache.hit", true);
            return Future.succeededFuture(TopupResponse.from(cached));
          }
          span.setAttribute("cache.hit", false);
          return repo.getTopupById(topupId)
              .compose(topup -> {
                if (topup == null) {
                  return Future.<Topup>failedFuture(new NotFoundException("Topup not found"));
                }
                return redisService.setJson(cacheKey, topup, CACHE_TTL).<Topup>map(v -> topup);
              })
              .map(TopupResponse::from);
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_by_id", "Topup fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch topup by id: {}", topupId, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_id", err.getMessage());
        });
  }

  @Override
  public Future<TopupResponse> getTopupByCardNumber(String cardNumber) {
    var tracingContext = tracingMetrics.startSpan("TopupQueryService.getTopupByCardNumber",
        Attributes.builder().put("topup.card_number", Objects.requireNonNull(cardNumber)).build());

    String cacheKey = CACHE_PREFIX + "card_single:" + cardNumber;

    return redisService.getJson(cacheKey, Topup.class)
        .compose(cached -> fetchTopupFromCacheOrDb(cardNumber, cacheKey, cached))
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card_single", "Topup found"))
        .onFailure(err -> {
          logger.error("Failed to fetch topup for card: {}", cardNumber, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_card_single", err.getMessage());
        });
  }

  private Future<TopupResponse> fetchTopupFromCacheOrDb(String cardNumber, String cacheKey, Topup cached) {
    if (cached != null) {
      return Future.succeededFuture(TopupResponse.from(cached));
    }

    return repo.getTopupByCardNumber(cardNumber)
        .compose(topup -> {
          if (topup == null) {
            return Future.failedFuture(new NotFoundException("Topup not found for card: " + cardNumber));
          }
          return redisService.setJson(cacheKey, topup, CACHE_TTL).map(v -> topup);
        })
        .map(TopupResponse::from);
  }
}