package io.example.saldo.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.domain.PagedResult;
import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.common.model.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.saldo.domain.requests.FindAllSaldos;
import io.example.saldo.domain.requests.MonthTotalSaldoBalance;
import io.example.saldo.model.Saldo;
import io.example.saldo.model.SaldoMonthBalanceResponse;
import io.example.saldo.model.SaldoMonthTotalBalanceResponse;
import io.example.saldo.model.SaldoResponse;
import io.example.saldo.model.SaldoResponseDeleteAt;
import io.example.saldo.model.SaldoYearBalanceResponse;
import io.example.saldo.model.SaldoYearTotalBalanceResponse;
import io.example.saldo.repository.SaldoQueryRepository;
import io.example.saldo.repository.SaldoStatsBalanceRepository;
import io.example.saldo.repository.SaldoStatsTotalRepository;
import io.example.saldo.service.SaldoQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import pb.saldo.Saldo.FindAllSaldoRequest;

public class SaldoQueryServiceImpl implements SaldoQueryService {
  private static final Logger logger = LoggerFactory.getLogger(SaldoQueryServiceImpl.class);

  private final SaldoQueryRepository repo;
  private final SaldoStatsTotalRepository repoStatsTotal;
  private final SaldoStatsBalanceRepository repoStatsBalance;

  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "saldo:";
  private static final Duration CACHE_TTL = Duration.ofHours(1);

  public SaldoQueryServiceImpl(
      SaldoQueryRepository repo,
      SaldoStatsTotalRepository repoStatsTotal,
      SaldoStatsBalanceRepository repoStatsBalance,
      RedisService redisService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.repoStatsTotal = repoStatsTotal;
    this.repoStatsBalance = repoStatsBalance;
    this.redisService = redisService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponsePagination<List<SaldoResponse>>> getAllSaldos(FindAllSaldoRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("SaldoQueryService.getAllSaldos");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    logger.info("Fetching saldos | search={}, page={}, pageSize={}", keyword, page, pageSize);

    String cacheKey = String.format("%sall:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redisService.getJson(cacheKey, ApiResponsePagination.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("saldo.cache_hit", true);
            tracingMetrics.completeSpanSuccess(tracingContext, "get_all", "Saldos fetched from cache");
            @SuppressWarnings("unchecked")
            ApiResponsePagination<List<SaldoResponse>> typedCached = (ApiResponsePagination<List<SaldoResponse>>) cached;
            return Future.succeededFuture(typedCached);
          }
          span.setAttribute("saldo.cache_hit", false);
          FindAllSaldos findAllReq = FindAllSaldos.builder()
              .search(keyword)
              .page(page)
              .pageSize(pageSize)
              .build();

          return repo.getSaldos(findAllReq)
              .map(result -> mapSaldoPagination(result, page, pageSize))
              .compose(response -> redisService.setJson(cacheKey, response, CACHE_TTL).map(response));
        })
        .onSuccess(response -> {
          span.setAttribute("saldos.count", (long) response.data().size());
          span.setAttribute("saldos.total_records", (long) response.pagination().totalRecords());
          tracingMetrics.completeSpanSuccess(tracingContext, "get_all", "Saldos fetched successfully");
        })
        .recover(throwable -> {
          logger.error("Failed to fetch saldos", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_all", throwable.getMessage());
          return Future
              .succeededFuture(ApiResponsePagination.error("Failed to fetch saldos: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<SaldoResponseDeleteAt>>> getActiveSaldos(FindAllSaldoRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("SaldoQueryService.getActiveSaldos");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    logger.info("Fetching active saldos | search={}, page={}, pageSize={}", keyword, page, pageSize);

    String cacheKey = String.format("%sactive:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redisService.getJson(cacheKey, ApiResponsePagination.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("saldo.cache_hit", true);
            tracingMetrics.completeSpanSuccess(tracingContext, "get_active", "Active saldos fetched from cache");
            @SuppressWarnings("unchecked")
            ApiResponsePagination<List<SaldoResponseDeleteAt>> typedCached = (ApiResponsePagination<List<SaldoResponseDeleteAt>>) cached;
            return Future.succeededFuture(typedCached);
          }
          span.setAttribute("saldo.cache_hit", false);
          FindAllSaldos findAllReq = FindAllSaldos.builder()
              .search(keyword)
              .page(page)
              .pageSize(pageSize)
              .build();

          return repo.getActiveSaldos(findAllReq)
              .map(result -> mapSaldoPaginationDeleteAt(result, page, pageSize))
              .compose(response -> redisService.setJson(cacheKey, response, CACHE_TTL).map(response));
        })
        .onSuccess(response -> {
          span.setAttribute("saldo.count", (long) response.data().size());
          span.setAttribute("saldo.total_records", (long) response.pagination().totalRecords());
          tracingMetrics.completeSpanSuccess(tracingContext, "get_active", "Active saldos fetched successfully");
        })
        .recover(throwable -> {
          logger.error("Failed to fetch active saldos", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_active", throwable.getMessage());
          return Future
              .succeededFuture(ApiResponsePagination.error("Failed to fetch active saldos: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<SaldoResponseDeleteAt>>> getTrashedSaldos(FindAllSaldoRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("SaldoQueryService.getTrashedSaldos");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    logger.info("Fetching trashed saldos | search={}, page={}, pageSize={}", keyword, page, pageSize);

    String cacheKey = String.format("%strashed:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redisService.getJson(cacheKey, ApiResponsePagination.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("saldo.cache_hit", true);
            tracingMetrics.completeSpanSuccess(tracingContext, "get_trashed", "Trashed saldos fetched from cache");
            @SuppressWarnings("unchecked")
            ApiResponsePagination<List<SaldoResponseDeleteAt>> typedCached = (ApiResponsePagination<List<SaldoResponseDeleteAt>>) cached;
            return Future.succeededFuture(typedCached);
          }
          span.setAttribute("saldo.cache_hit", false);
          FindAllSaldos findAllReq = FindAllSaldos.builder()
              .search(keyword)
              .page(page)
              .pageSize(pageSize)
              .build();

          return repo.getTrashedSaldos(findAllReq)
              .map(result -> mapSaldoPaginationDeleteAt(result, page, pageSize))
              .compose(response -> redisService.setJson(cacheKey, response, CACHE_TTL).map(response));
        })
        .onSuccess(response -> {
          span.setAttribute("saldo.count", (long) response.data().size());
          span.setAttribute("saldo.total_records", (long) response.pagination().totalRecords());
          tracingMetrics.completeSpanSuccess(tracingContext, "get_trashed", "Trashed saldos fetched successfully");
        })
        .recover(throwable -> {
          logger.error("Failed to fetch trashed saldos", throwable);
          tracingMetrics.completeSpanError(tracingContext, "get_trashed", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.error("Failed to fetch trashed saldos: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<SaldoResponse>> getSaldoByCardNumber(String cardNumber) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoQueryService.getSaldoByCardNumber",
        Attributes.builder()
            .put("saldo.cardNumber", Objects.requireNonNull(cardNumber))
            .build());

    logger.info("Fetching saldo by card number: {}", cardNumber);

    return repo.getSaldoByCardNumber(cardNumber)
        .map(saldo -> {
          if (saldo == null) {
            return ApiResponse.<SaldoResponse>error("Saldo not found");
          }
          tracingMetrics.completeSpanSuccess(tracingContext, "get_by_card_number", "Saldo fetched successfully");
          return ApiResponse.success("Saldo fetched successfully", SaldoResponse.from(saldo));
        })
        .recover(err -> {
          logger.error("Failed to fetch saldo by card number: {}", cardNumber, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_card_number", err.getMessage());
          return Future.succeededFuture(ApiResponse.<SaldoResponse>error("Failed to fetch saldo: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<SaldoResponse>> getSaldoById(Integer saldoId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("SaldoQueryService.getSaldoById",
        Attributes.builder().put("saldo.id", (long) saldoId).build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));
    logger.info("Fetching saldo by id: {}", saldoId);

    String cacheKey = CACHE_PREFIX + saldoId;

    return redisService.get(cacheKey)
        .compose(cachedSaldo -> {
          if (cachedSaldo != null && !cachedSaldo.isEmpty()) {
            logger.info("Saldo {} found in cache", saldoId);
            span.setAttribute("saldo.cache_hit", true);
            try {
              Saldo saldo = Saldo.fromJson(new JsonObject(cachedSaldo));
              tracingMetrics.completeSpanSuccess(tracingContext, "get_by_id", "Saldo fetched from cache");
              return Future.succeededFuture(
                  ApiResponse.success("Saldo fetched successfully (from cache)", SaldoResponse.from(saldo)));
            } catch (Exception e) {
              logger.warn("Failed to parse cached saldo data for saldo {}: {}", saldoId, e.getMessage());
              return fetchSaldoFromDatabase(saldoId, tracingContext);
            }
          } else {
            span.setAttribute("saldo.cache_hit", false);
            return fetchSaldoFromDatabase(saldoId, tracingContext);
          }
        })
        .recover(err -> {
          logger.error("Failed to fetch saldo by id: {}", saldoId, err);
          tracingMetrics.completeSpanError(tracingContext, "get_by_id", err.getMessage());
          return Future.succeededFuture(ApiResponse.<SaldoResponse>error("Failed to fetch saldo: " + err.getMessage()));
        });
  }

  private Future<ApiResponse<SaldoResponse>> fetchSaldoFromDatabase(Integer saldoId,
      TracingMetrics.TracingContext tracingContext) {
    logger.info("Fetching saldo {} from database", saldoId);
    String cacheKey = CACHE_PREFIX + saldoId;

    return repo.getSaldoById(saldoId)
        .compose(saldo -> {
          if (saldo == null)
            return Future.failedFuture(new NotFoundException("Saldo not found"));
          return redisService.setJson(cacheKey, saldo.toJson(), CACHE_TTL).map(v -> saldo);
        })
        .map(saldo -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "get_by_id", "Saldo fetched from database");
          return ApiResponse.success("Saldo fetched successfully", SaldoResponse.from(saldo));
        })
        .recover(err -> {
          logger.error("Failed to fetch saldo {} from database: {}", saldoId, err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Saldo not found: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<List<SaldoMonthTotalBalanceResponse>>> getMonthlyTotalSaldoBalance(
      MonthTotalSaldoBalance req) {
    String cacheKey = String.format("saldo:total_balance:month:%d:%d", req.getYear(), req.getMonth());
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("SaldoQueryService.getMonthlyTotalSaldoBalance");

    MonthTotalSaldoBalance domainReq = MonthTotalSaldoBalance.builder()
        .year(req.getYear())
        .month(req.getMonth())
        .build();

    return redisService.getJsonList(cacheKey, SaldoMonthTotalBalanceResponse.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            tracingMetrics.completeSpanSuccess(tracingContext, "get_monthly_total_balance", "Data from cache");
            return Future.succeededFuture(cached);
          }
          return fetchFromDbMapAndCache(
              repoStatsTotal.getMonthlyTotalSaldoBalance(domainReq),
              cacheKey, tracingContext, "get_monthly_total_balance", SaldoMonthTotalBalanceResponse::from);
        })
        .map(results -> ApiResponse.success("Monthly total balance fetched successfully", results))
        .recover(err -> Future
            .succeededFuture(ApiResponse.error("Failed to get monthly total balance: " + err.getMessage())));
  }

  @Override
  public Future<ApiResponse<List<SaldoYearTotalBalanceResponse>>> getYearlyTotalSaldoBalances(Integer endYear) {
    String cacheKey = String.format("saldo:total_balance:year:%d", endYear);
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("SaldoQueryService.getYearlyTotalSaldoBalances");

    return redisService.getJsonList(cacheKey, SaldoYearTotalBalanceResponse.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            tracingMetrics.completeSpanSuccess(tracingContext, "get_yearly_total_balances", "Data from cache");
            return Future.succeededFuture(cached);
          }
          return fetchFromDbMapAndCache(
              repoStatsTotal.getYearlyTotalSaldoBalances(endYear),
              cacheKey, tracingContext, "get_yearly_total_balances", SaldoYearTotalBalanceResponse::from);
        })
        .map(results -> ApiResponse.success("Yearly total balances fetched successfully", results))
        .recover(err -> Future
            .succeededFuture(ApiResponse.error("Failed to get yearly total balances: " + err.getMessage())));
  }

  @Override
  public Future<ApiResponse<List<SaldoMonthBalanceResponse>>> getMonthlySaldoBalances(Integer year) {
    String cacheKey = String.format("saldo:balance:month:%d", year);
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("SaldoQueryService.getMonthlySaldoBalances");

    return redisService.getJsonList(cacheKey, SaldoMonthBalanceResponse.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            tracingMetrics.completeSpanSuccess(tracingContext, "get_monthly_balances", "Data from cache");
            return Future.succeededFuture(cached);
          }
          return fetchFromDbMapAndCache(
              repoStatsBalance.getMonthlySaldoBalances(year),
              cacheKey, tracingContext, "get_monthly_balances", SaldoMonthBalanceResponse::from);
        })
        .map(results -> ApiResponse.success("Monthly balances fetched successfully", results))
        .recover(
            err -> Future.succeededFuture(ApiResponse.error("Failed to get monthly balances: " + err.getMessage())));
  }

  @Override
  public Future<ApiResponse<List<SaldoYearBalanceResponse>>> getYearlySaldoBalances(Integer endYear) {
    String cacheKey = String.format("saldo:balance:year:%d", endYear);
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("SaldoQueryService.getYearlySaldoBalances");

    return redisService.getJsonList(cacheKey, SaldoYearBalanceResponse.class)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            tracingMetrics.completeSpanSuccess(tracingContext, "get_yearly_balances", "Data from cache");
            return Future.succeededFuture(cached);
          }
          return fetchFromDbMapAndCache(
              repoStatsBalance.getYearlySaldoBalances(endYear),
              cacheKey, tracingContext, "get_yearly_balances", SaldoYearBalanceResponse::from);
        })
        .map(results -> ApiResponse.success("Yearly balances fetched successfully", results))
        .recover(
            err -> Future.succeededFuture(ApiResponse.error("Failed to get yearly balances: " + err.getMessage())));
  }

  private <T, R> Future<List<R>> fetchFromDbMapAndCache(
      Future<List<T>> dbResultFuture,
      String cacheKey,
      TracingMetrics.TracingContext tracingContext,
      String successSpanName,
      Function<T, R> mapper) {
    return dbResultFuture
        .compose(dbResults -> {
          List<R> responseList = dbResults.stream().map(mapper).collect(Collectors.toList());
          return redisService.setJsonList(cacheKey, responseList, Duration.ofHours(6)).map(v -> responseList);
        })
        .onSuccess(results -> tracingMetrics.completeSpanSuccess(tracingContext, successSpanName, "Data cached"))
        .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, successSpanName, err.getMessage()));
  }

  private ApiResponsePagination<List<SaldoResponse>> mapSaldoPagination(PagedResult<Saldo> result, int page,
      int pageSize) {
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<SaldoResponse> data = result.getData().stream().map(SaldoResponse::from).toList();
    return new ApiResponsePagination<>("success", "Saldos found", data,
        new PaginationMeta(page, pageSize, totalPages, totalRecords));
  }

  private ApiResponsePagination<List<SaldoResponseDeleteAt>> mapSaldoPaginationDeleteAt(PagedResult<Saldo> result,
      int page, int pageSize) {
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<SaldoResponseDeleteAt> data = result.getData().stream().map(SaldoResponseDeleteAt::from).toList();
    return new ApiResponsePagination<>("success", "Saldos found", data,
        new PaginationMeta(page, pageSize, totalPages, totalRecords));
  }
}
