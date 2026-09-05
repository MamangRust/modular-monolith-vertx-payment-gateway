package io.example.saldo.service.impl;

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
import io.example.saldo.domain.requests.FindAllSaldos;
import io.example.saldo.model.Saldo;
import io.example.saldo.model.SaldoResponse;
import io.example.saldo.model.SaldoResponseDeleteAt;
import io.example.saldo.repository.SaldoQueryRepository;
import io.example.saldo.service.SaldoQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.saldo.Saldo.FindAllSaldoRequest;

@RequiredArgsConstructor
public class SaldoQueryServiceImpl implements SaldoQueryService {
  private static final Logger logger = LoggerFactory.getLogger(SaldoQueryServiceImpl.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final SaldoQueryRepository repo;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "saldo:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  private PagedResult<SaldoResponse> mapSaldoPagination(PagedResult<Saldo> result) {
    List<SaldoResponse> data = result.getData().stream().map(SaldoResponse::from).toList();
    return new PagedResult<>(data, result.getTotalRecords());
  }

  private PagedResult<SaldoResponseDeleteAt> mapSaldoPaginationDeleteAt(PagedResult<Saldo> result) {
    List<SaldoResponseDeleteAt> data = result.getData().stream().map(SaldoResponseDeleteAt::from).toList();
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
  public Future<PagedResult<SaldoResponse>> getAllSaldos(FindAllSaldoRequest req) {
    var ctx = metrics.startSpan("SaldoQueryService.getAllSaldos");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%sall:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    // Bypass Redis cache for paginated queries — PagedResult<T> loses generic
    // type info during deserialization, causing ClassCastException.
    span.setAttribute("saldo.cache_hit", false);
    return repo.getSaldos(FindAllSaldos.builder().page(page).pageSize(pageSize).search(keyword).build())
        .map(this::mapSaldoPagination)
        .onSuccess(resp -> {
          span.setAttribute("saldos.count", (long) resp.getData().size());
          span.setAttribute("saldos.total_records", (long) resp.getTotalRecords());
          metrics.completeSpanSuccess(ctx, "get_all", "Saldos fetched successfully");
        })
        .onFailure(err -> {
          logger.error("Failed to fetch saldos", err);
          metrics.completeSpanError(ctx, "get_all", err.getMessage());
        });
  }

  @Override
  public Future<PagedResult<SaldoResponseDeleteAt>> getActiveSaldos(FindAllSaldoRequest req) {
    var ctx = metrics.startSpan("SaldoQueryService.getActiveSaldos");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%sactive:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("saldo.cache_hit", true);
              PagedResult<Saldo> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Saldo>>() {
              });
              return Future.succeededFuture(mapSaldoPaginationDeleteAt(typedCached));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached active saldos: {}", e.getMessage());
            }
          }
          span.setAttribute("saldo.cache_hit", false);
          return repo.getActiveSaldos(FindAllSaldos.builder().page(page).pageSize(pageSize).search(keyword).build())
              .compose(result -> redis.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(this::mapSaldoPaginationDeleteAt);
        })
        .onSuccess(resp -> {
          span.setAttribute("saldos.count", (long) resp.getData().size());
          span.setAttribute("saldos.total_records", (long) resp.getTotalRecords());
          metrics.completeSpanSuccess(ctx, "get_active", "Active saldos fetched successfully");
        })
        .onFailure(err -> {
          logger.error("Failed to fetch active saldos", err);
          metrics.completeSpanError(ctx, "get_active", err.getMessage());
        });
  }

  @Override
  public Future<PagedResult<SaldoResponseDeleteAt>> getTrashedSaldos(FindAllSaldoRequest req) {
    var ctx = metrics.startSpan("SaldoQueryService.getTrashedSaldos");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%strashed:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("saldo.cache_hit", true);
              PagedResult<Saldo> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<Saldo>>() {
              });
              return Future.succeededFuture(mapSaldoPaginationDeleteAt(typedCached));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached trashed saldos: {}", e.getMessage());
            }
          }
          span.setAttribute("saldo.cache_hit", false);
          return repo.getTrashedSaldos(FindAllSaldos.builder().page(page).pageSize(pageSize).search(keyword).build())
              .compose(result -> redis.setJson(cacheKey, result, CACHE_TTL).map(v -> result))
              .map(this::mapSaldoPaginationDeleteAt);
        })
        .onSuccess(resp -> {
          span.setAttribute("saldos.count", (long) resp.getData().size());
          span.setAttribute("saldos.total_records", (long) resp.getTotalRecords());
          metrics.completeSpanSuccess(ctx, "get_trashed", "Trashed saldos fetched successfully");
        })
        .onFailure(err -> {
          logger.error("Failed to fetch trashed saldos", err);
          metrics.completeSpanError(ctx, "get_trashed", err.getMessage());
        });
  }

  @Override
  public Future<SaldoResponse> getSaldoByCardNumber(String cardNumber) {
    var ctx = metrics.startSpan(
        "SaldoQueryService.getSaldoByCardNumber",
        Attributes.builder().put("saldo.cardNumber", Objects.requireNonNull(cardNumber)).build());

    return repo.getSaldoByCardNumber(cardNumber)
        .compose(saldo -> {
          if (saldo == null)
            return Future.failedFuture(new NotFoundException("Saldo not found"));
          return Future.succeededFuture(SaldoResponse.from(saldo));
        })
        .onSuccess(v -> metrics.completeSpanSuccess(ctx, "get_by_card_number", "Saldo fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch saldo by card number: {}", cardNumber, err);
          metrics.completeSpanError(ctx, "get_by_card_number", err.getMessage());
        });
  }

  @Override
  public Future<SaldoResponse> getSaldoById(Integer saldoId) {
    var ctx = metrics.startSpan(
        "SaldoQueryService.getSaldoById",
        Attributes.builder().put("saldo.id", (long) saldoId).build());
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    String cacheKey = CACHE_PREFIX + "id:" + saldoId;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("saldo.cache_hit", true);
              Saldo saldo = mapper.readValue(jsonStr, Saldo.class);
              return Future.succeededFuture(SaldoResponse.from(saldo));
            } catch (Exception e) {
              logger.warn("Failed to deserialize cached saldo: {}", e.getMessage());
            }
          }
          span.setAttribute("saldo.cache_hit", false);
          return repo.getSaldoById(saldoId)
              .compose(saldo -> {
                if (saldo == null)
                  return Future.failedFuture(new NotFoundException("Saldo not found"));
                return redis.setJson(cacheKey, saldo, CACHE_TTL).map(v -> SaldoResponse.from(saldo));
              });
        })
        .onSuccess(v -> metrics.completeSpanSuccess(ctx, "get_by_id", "Saldo fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch saldo by id: {}", saldoId, err);
          metrics.completeSpanError(ctx, "get_by_id", err.getMessage());
        });
  }
}