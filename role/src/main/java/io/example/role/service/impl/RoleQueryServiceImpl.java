package io.example.role.service.impl;

import java.time.Duration;
import java.util.List;
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
import io.example.role.model.Role;
import io.example.role.model.RoleResponse;
import io.example.role.model.RoleResponseDeleteAt;
import io.example.role.repository.RoleQueryRepository;
import io.example.role.service.RoleQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import pb.role.Role.FindAllRoleRequest;

public class RoleQueryServiceImpl implements RoleQueryService {
  private static final Logger logger = LoggerFactory.getLogger(RoleQueryServiceImpl.class);

  private final RoleQueryRepository repo;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "role:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public RoleQueryServiceImpl(
      RoleQueryRepository repo,
      RedisService redis,
      TracingMetrics metrics) {
    this.repo = repo;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<ApiResponsePagination<List<RoleResponse>>> getAllRoles(
      FindAllRoleRequest req) {

    TracingMetrics.TracingContext tracingContext = metrics.startSpan("RoleQueryService.getAllRoles");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    String cacheKey = String.format("%sall:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redis.getJson(cacheKey, ApiResponsePagination.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("role.cache_hit", true);
            metrics.completeSpanSuccess(tracingContext, "get_all", "Roles fetched from cache");
            @SuppressWarnings("unchecked")
            ApiResponsePagination<List<RoleResponse>> typedCached = (ApiResponsePagination<List<RoleResponse>>) cached;
            return Future.succeededFuture(typedCached);
          }
          span.setAttribute("role.cache_hit", false);
          return repo.getRoles(keyword, page, pageSize)
              .map(result -> mapRolePagination(result, page, pageSize))
              .compose(response -> redis.setJson(cacheKey, response, CACHE_TTL).map(response));
        })
        .onSuccess(response -> {
          ApiResponsePagination<List<RoleResponse>> typedResponse = (ApiResponsePagination<List<RoleResponse>>) response;
          span.setAttribute("roles.count", (long) typedResponse.data().size());
          span.setAttribute("roles.total_records", (long) typedResponse.pagination().totalRecords());
          metrics.completeSpanSuccess(tracingContext, "get_all", "Roles fetched successfully");
        })
        .recover(throwable -> {
          logger.error("Failed to fetch roles", throwable);
          metrics.completeSpanError(tracingContext, "get_all", throwable.getMessage());

          return Future.succeededFuture(
              ApiResponsePagination.<List<RoleResponse>>error("Failed to fetch roles: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<RoleResponseDeleteAt>>> getActiveRoles(
      FindAllRoleRequest req) {

    TracingMetrics.TracingContext tracingContext = metrics.startSpan("RoleQueryService.getActiveRoles");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    String cacheKey = String.format("%sactive:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redis.getJson(cacheKey, ApiResponsePagination.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("role.cache_hit", true);
            metrics.completeSpanSuccess(tracingContext, "get_active", "Active roles fetched from cache");
            @SuppressWarnings("unchecked")
            ApiResponsePagination<List<RoleResponseDeleteAt>> typedCached = (ApiResponsePagination<List<RoleResponseDeleteAt>>) cached;
            return Future.succeededFuture(typedCached);
          }
          span.setAttribute("role.cache_hit", false);
          return repo.getActiveRoles(keyword, page, pageSize)
              .map(result -> mapRolePaginationDeleteAt(result, page, pageSize))
              .compose(response -> redis.setJson(cacheKey, response, CACHE_TTL).map(response));
        })
        .onSuccess(response -> {
          ApiResponsePagination<List<RoleResponseDeleteAt>> typedResponse = (ApiResponsePagination<List<RoleResponseDeleteAt>>) response;
          span.setAttribute("roles.count", (long) typedResponse.data().size());
          span.setAttribute("roles.total_records", (long) typedResponse.pagination().totalRecords());
          metrics.completeSpanSuccess(tracingContext, "get_active", "Active roles fetched successfully");
        })
        .recover(throwable -> {
          logger.error("Failed to fetch active roles", throwable);
          metrics.completeSpanError(tracingContext, "get_active", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.<List<RoleResponseDeleteAt>>error(
                  "Failed to fetch active roles: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponsePagination<List<RoleResponseDeleteAt>>> getTrashedRoles(
      FindAllRoleRequest req) {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan("RoleQueryService.getTrashedRoles");
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    String cacheKey = String.format("%strashed:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redis.getJson(cacheKey, ApiResponsePagination.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("role.cache_hit", true);
            metrics.completeSpanSuccess(tracingContext, "get_trashed", "Trashed roles fetched from cache");
            @SuppressWarnings("unchecked")
            ApiResponsePagination<List<RoleResponseDeleteAt>> typedCached = (ApiResponsePagination<List<RoleResponseDeleteAt>>) cached;
            return Future.succeededFuture(typedCached);
          }
          span.setAttribute("role.cache_hit", false);
          return repo.getTrashedRoles(keyword, page, pageSize)
              .map(result -> mapRolePaginationDeleteAt(result, page, pageSize))
              .compose(response -> redis.setJson(cacheKey, response, CACHE_TTL).map(response));
        })
        .onSuccess(response -> {
          ApiResponsePagination<List<RoleResponseDeleteAt>> typedResponse = (ApiResponsePagination<List<RoleResponseDeleteAt>>) response;
          span.setAttribute("roles.count", (long) typedResponse.data().size());
          span.setAttribute("roles.total_records", (long) typedResponse.pagination().totalRecords());
          metrics.completeSpanSuccess(tracingContext, "get_trashed", "Trashed roles fetched successfully");
        })
        .recover(throwable -> {
          logger.error("Failed to fetch trashed roles", throwable);
          metrics.completeSpanError(tracingContext, "get_trashed", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponsePagination.<List<RoleResponseDeleteAt>>error(
                  "Failed to fetch trashed roles: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<RoleResponse>> getRoleById(Integer roleId) {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan(
        "RoleQueryService.getRoleById",
        Attributes.builder()
            .put("role.id", (long) roleId)
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Fetching role by id: {}", roleId);
    String cacheKey = CACHE_PREFIX + "id:" + roleId;

    return redis.getJson(cacheKey, Role.class)
        .compose(cachedRole -> {
          if (cachedRole != null) {
            logger.info("Role {} found in cache", roleId);
            span.setAttribute("role.cache_hit", true);
            metrics.completeSpanSuccess(tracingContext, "get_by_id", "Role fetched from cache");
            return Future.succeededFuture(ApiResponse.success(
                "Role fetched successfully (from cache)",
                RoleResponse.from(cachedRole)));
          } else {
            span.setAttribute("role.cache_hit", false);
            return repo.getRoleById(roleId)
                .compose(role -> {
                  if (role == null) {
                    return Future.failedFuture(new NotFoundException("Role not found"));
                  }
                  return redis.setJson(cacheKey, role, CACHE_TTL).map(role);
                })
                .map(role -> {
                  metrics.completeSpanSuccess(tracingContext, "get_by_id", "Role fetched from database");
                  return ApiResponse.success("Role fetched successfully", RoleResponse.from(role));
                });
          }
        })
        .recover(err -> {
          logger.error("Failed to fetch role by id: {}", roleId, err);
          metrics.completeSpanError(tracingContext, "get_by_id", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<RoleResponse>error(
                  "Failed to fetch role: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<List<RoleResponse>>> getRolesByUserId(Integer userId) {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan(
        "RoleQueryService.getRolesByUserId",
        Attributes.builder()
            .put("user.id", (long) userId)
            .build());

    logger.info("Fetching roles for user ID: {}", userId);
    String cacheKey = CACHE_PREFIX + "user:" + userId;

    return redis.getJsonList(cacheKey, Role.class)
        .compose(cachedRoles -> {
          if (cachedRoles != null && !cachedRoles.isEmpty()) {
            logger.info("Roles for user {} found in cache", userId);
            metrics.completeSpanSuccess(tracingContext, "get_by_user_id", "Roles fetched from cache");
            return Future.succeededFuture(ApiResponse.success(
                "Roles for user fetched successfully (from cache)",
                cachedRoles.stream().map(RoleResponse::from).toList()));
          } else {
            return repo.getRolesByUserId(userId)
                .compose(roles -> {
                  if (roles == null || roles.isEmpty()) {
                    return Future.succeededFuture(List.<Role>of());
                  }
                  return redis.setJsonList(cacheKey, roles, CACHE_TTL).map(roles);
                })
                .map(roles -> {
                  metrics.completeSpanSuccess(tracingContext, "get_by_user_id", "Roles fetched from database");
                  return ApiResponse.success("Roles for user fetched successfully",
                      roles.stream().map(RoleResponse::from).toList());
                });
          }
        })
        .recover(err -> {
          logger.error("Failed to fetch roles for user ID: {}", userId, err);
          metrics.completeSpanError(tracingContext, "get_by_user_id", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<List<RoleResponse>>error("Failed to fetch roles for user: " + err.getMessage()));
        });
  }

  private ApiResponsePagination<List<RoleResponse>> mapRolePagination(
      PagedResult<Role> result,
      int page,
      int pageSize) {

    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<RoleResponse> data = result.getData()
        .stream()
        .map(RoleResponse::from)
        .toList();

    return new ApiResponsePagination<>(
        "success",
        "Roles found",
        data,
        new PaginationMeta(
            page,
            pageSize,
            totalPages,
            totalRecords));
  }

  private ApiResponsePagination<List<RoleResponseDeleteAt>> mapRolePaginationDeleteAt(
      PagedResult<Role> result,
      int page,
      int pageSize) {

    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<RoleResponseDeleteAt> data = result.getData()
        .stream()
        .map(RoleResponseDeleteAt::from)
        .toList();

    return new ApiResponsePagination<>(
        "success",
        "Roles found",
        data,
        new PaginationMeta(
            page,
            pageSize,
            totalPages,
            totalRecords));
  }
}
