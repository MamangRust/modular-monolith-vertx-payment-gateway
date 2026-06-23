package io.example.merchant.service.impl;

import java.time.Duration;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.model.MerchantTransactions;
import io.example.merchant.repository.MerchantTransactionRepository;
import io.example.merchant.service.MerchantTransactionService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;
import pb.merchant.Merchant.FindAllMerchantTransactionId;

@RequiredArgsConstructor
public class MerchantTransactionServiceImpl implements MerchantTransactionService {
  private static final Logger logger = LoggerFactory.getLogger(MerchantTransactionServiceImpl.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final MerchantTransactionRepository repo;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final String CACHE_PREFIX = "transactions:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(5);

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
  public Future<PagedResult<MerchantTransactions>> getTransactions(FindAllMerchantTransaction req) {
    var ctx = metrics.startSpan("MerchantTransactionService.getTransactions");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%sall:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("transaction.cache_hit", true);
              PagedResult<MerchantTransactions> cached = mapper.readValue(jsonStr,
                  new TypeReference<PagedResult<MerchantTransactions>>() {
                  });
              return Future.succeededFuture(cached);
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached transactions: {}", e.getMessage());
            }
          }
          span.setAttribute("transaction.cache_hit", false);
          return repo.findAllTransactionMerchant(req)
              .compose(result -> redis.setJson(cacheKey, result, CACHE_TTL).map(v -> result));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTransactions", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getTransactions", e.getMessage()));
  }

  @Override
  public Future<PagedResult<MerchantTransactions>> getTransactionsByApiKey(FindAllMerchantTransactionApikey req) {
    var ctx = metrics.startSpan("MerchantTransactionService.getTransactionsByApiKey");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%sapikey:%s:p:%d:s:%d:k:%s", CACHE_PREFIX, req.getApiKey(), page, pageSize,
        keyword);

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("transaction.cache_hit", true);
              PagedResult<MerchantTransactions> cached = mapper.readValue(jsonStr,
                  new TypeReference<PagedResult<MerchantTransactions>>() {
                  });
              return Future.succeededFuture(cached);
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached transactions by apikey: {}", e.getMessage());
            }
          }
          span.setAttribute("transaction.cache_hit", false);
          return repo.findAllTransactionByApikey(req)
              .compose(result -> redis.setJson(cacheKey, result, CACHE_TTL).map(v -> result));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTransactionsByApiKey", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getTransactionsByApiKey", e.getMessage()));
  }

  @Override
  public Future<PagedResult<MerchantTransactions>> getTransactionsByMerchantId(FindAllMerchantTransactionId req) {
    var ctx = metrics.startSpan("MerchantTransactionService.getTransactionsByMerchantId");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%smerchant:%d:p:%d:s:%d:k:%s", CACHE_PREFIX, req.getId(), page, pageSize, keyword);

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("transaction.cache_hit", true);
              PagedResult<MerchantTransactions> cached = mapper.readValue(jsonStr,
                  new TypeReference<PagedResult<MerchantTransactions>>() {
                  });
              return Future.succeededFuture(cached);
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached transactions by merchantId: {}", e.getMessage());
            }
          }
          span.setAttribute("transaction.cache_hit", false);
          return repo.findAllTransactionByMerchant(req)
              .compose(result -> redis.setJson(cacheKey, result, CACHE_TTL).map(v -> result));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTransactionsByMerchantId", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getTransactionsByMerchantId", e.getMessage()));
  }
}