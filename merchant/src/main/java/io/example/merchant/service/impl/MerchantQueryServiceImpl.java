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
import io.example.merchant.model.Merchant;
import io.example.merchant.model.MerchantResponse;
import io.example.merchant.model.MerchantResponseDeleteAt;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.service.MerchantQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.FindAllMerchantRequest;

@RequiredArgsConstructor
public class MerchantQueryServiceImpl implements MerchantQueryService {
  private static final Logger logger = LoggerFactory.getLogger(MerchantQueryServiceImpl.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final MerchantQueryRepository repo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;
  private static final String CACHE_PREFIX = "merchant:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  private PagedResult<MerchantResponse> mapMerchantPagination(PagedResult<Merchant> result) {
    List<MerchantResponse> data = result.getData().stream().map(MerchantResponse::from).toList();
    return new PagedResult<>(data, result.getTotalRecords());
  }

  private PagedResult<MerchantResponseDeleteAt> mapMerchantPaginationDeleteAt(PagedResult<Merchant> result) {
    List<MerchantResponseDeleteAt> data = result.getData().stream().map(MerchantResponseDeleteAt::from).toList();
    return new PagedResult<>(data, result.getTotalRecords());
  }

  private int safePage(int page) {
    return page > 0 ? page : 1;
  }

  private int safePageSize(int size) {
    return size > 0 ? size : 10;
  }

  private String safeKeyword(String search) {
    return (search != null && !search.isEmpty()) ? search : "";
  }

  @Override
  public Future<PagedResult<MerchantResponse>> findAll(FindAllMerchantRequest req) {
    var ctx = tracingMetrics.startSpan("MerchantQueryService.findAll");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%sall:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant.cache_hit", true);
              PagedResult<Merchant> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Merchant>>() {
              });
              return Future.succeededFuture(mapMerchantPagination(typedCached));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached merchants: {}", e.getMessage());
            }
          }
          span.setAttribute("merchant.cache_hit", false);
          return repo.findAllMerchants(req)
              .compose(result -> redisService.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(this::mapMerchantPagination);
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findAll", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findAll", e.getMessage()));
  }

  @Override
  public Future<MerchantResponse> findById(int merchantId) {
    var ctx = tracingMetrics.startSpan("MerchantQueryService.findById",
        Attributes.builder().put("merchant.id", (long) merchantId).build());
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    String cacheKey = CACHE_PREFIX + "id:" + merchantId;

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant.cache_hit", true);
              Merchant merchant = mapper.readValue(jsonStr, Merchant.class);
              return Future.succeededFuture(MerchantResponse.from(merchant));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached merchant: {}", e.getMessage());
            }
          }
          span.setAttribute("merchant.cache_hit", false);
          return repo.findByMerchantId(merchantId)
              .compose(merchant -> {
                if (merchant == null)
                  return Future.failedFuture(new NotFoundException("Merchant not found"));
                return redisService.setJson(cacheKey, merchant, CACHE_TTL).map(v -> MerchantResponse.from(merchant));
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findById", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findById", e.getMessage()));
  }

  @Override
  public Future<PagedResult<MerchantResponseDeleteAt>> findByActive(FindAllMerchantRequest req) {
    var ctx = tracingMetrics.startSpan("MerchantQueryService.findByActive");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%sactive:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant.cache_hit", true);
              PagedResult<Merchant> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Merchant>>() {
              });
              return Future.succeededFuture(mapMerchantPaginationDeleteAt(typedCached));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached active merchants: {}", e.getMessage());
            }
          }
          span.setAttribute("merchant.cache_hit", false);
          return repo.findByActive(req)
              .compose(result -> redisService.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(this::mapMerchantPaginationDeleteAt);
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findByActive", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findByActive", e.getMessage()));
  }

  @Override
  public Future<PagedResult<MerchantResponseDeleteAt>> findByTrashed(FindAllMerchantRequest req) {
    var ctx = tracingMetrics.startSpan("MerchantQueryService.findByTrashed");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%strashed:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant.cache_hit", true);
              PagedResult<Merchant> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Merchant>>() {
              });
              return Future.succeededFuture(mapMerchantPaginationDeleteAt(typedCached));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached trashed merchants: {}", e.getMessage());
            }
          }
          span.setAttribute("merchant.cache_hit", false);
          return repo.findByTrashed(req)
              .compose(result -> redisService.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(this::mapMerchantPaginationDeleteAt);
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findByTrashed", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findByTrashed", e.getMessage()));
  }

  @Override
  public Future<MerchantResponse> findByApiKey(String apiKey) {
    var ctx = tracingMetrics.startSpan("MerchantQueryService.findByApiKey");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "apikey:" + apiKey;

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant.cache_hit", true);
              Merchant merchant = mapper.readValue(jsonStr, Merchant.class);
              return Future.succeededFuture(MerchantResponse.from(merchant));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached merchant by apiKey: {}", e.getMessage());
            }
          }
          span.setAttribute("merchant.cache_hit", false);
          return repo.findByApiKey(apiKey)
              .compose(merchant -> {
                if (merchant == null)
                  return Future.failedFuture(new NotFoundException("Merchant not found"));
                return redisService.setJson(cacheKey, merchant, CACHE_TTL).map(v -> MerchantResponse.from(merchant));
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findByApiKey", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findByApiKey", e.getMessage()));
  }

  @Override
  public Future<List<MerchantResponse>> findByMerchantUserId(Integer userId) {
    var ctx = tracingMetrics.startSpan("MerchantQueryService.findByMerchantUserId");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = CACHE_PREFIX + "user:" + userId;

    return redisService.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("merchant.cache_hit", true);
              List<Merchant> list = mapper.readValue(jsonStr, new TypeReference<List<Merchant>>() {
              });
              return Future.succeededFuture(list.stream().map(MerchantResponse::from).toList());
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached merchants by userId: {}", e.getMessage());
            }
          }
          span.setAttribute("merchant.cache_hit", false);
          return repo.findByMerchantUserId(userId)
              .compose(list -> {
                if (list == null || list.isEmpty()) {
                  return Future.succeededFuture(List.<MerchantResponse>of());
                }
                return redisService.setJson(cacheKey, list, CACHE_TTL)
                    .map(v -> list.stream().map(MerchantResponse::from).toList());
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findByMerchantUserId", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findByMerchantUserId", e.getMessage()));
  }
}