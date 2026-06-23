package io.example.role.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.domain.PagedResult;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.role.domain.requests.FindAllRoles;
import io.example.role.model.Role;
import io.example.role.model.RoleResponse;
import io.example.role.model.RoleResponseDeleteAt;
import io.example.role.repository.RoleQueryRepository;
import io.example.role.service.RoleQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoleQueryServiceImpl implements RoleQueryService {
  private static final Logger logger = LoggerFactory.getLogger(RoleQueryServiceImpl.class);

  private final RoleQueryRepository repo;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "role:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  private PagedResult<RoleResponse> mapRolePagination(PagedResult<Role> result, int page, int pageSize) {
    int totalRecords = result.getTotalRecords();
    List<RoleResponse> data = result.getData().stream().map(RoleResponse::from).toList();

    return new PagedResult<>(data, totalRecords);
  }

  private PagedResult<RoleResponseDeleteAt> mapRolePaginationDeleteAt(PagedResult<Role> result, int page,
      int pageSize) {
    int totalRecords = result.getTotalRecords();
    List<RoleResponseDeleteAt> data = result.getData().stream().map(RoleResponseDeleteAt::from).toList();

    return new PagedResult<>(data, totalRecords);
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
  public Future<PagedResult<RoleResponse>> getAllRoles(FindAllRoles req) {
    TracingMetrics.TracingContext ctx = metrics.startSpan("RoleQueryService.getAllRoles");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%sall:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redis.getJson(cacheKey, PagedResult.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("role.cache_hit", true);
            @SuppressWarnings("unchecked")
            PagedResult<Role> typedCached = (PagedResult<Role>) cached;
            return Future.succeededFuture(mapRolePagination(typedCached, page, pageSize));
          }
          span.setAttribute("role.cache_hit", false);
          return repo.getRoles(req)
              .compose(result -> redis.setJson(cacheKey, result, CACHE_TTL).map(result))
              .map(result -> mapRolePagination(result, page, pageSize));
        })
        .onSuccess(resp -> {
          span.setAttribute("roles.count", (long) resp.getData().size());
          span.setAttribute("roles.total_records", (long) resp.getTotalRecords());
          metrics.completeSpanSuccess(ctx, "get_all", "Roles fetched successfully");
        })
        .onFailure(err -> {
          logger.error("Failed to fetch roles", err);
          metrics.completeSpanError(ctx, "get_all", err.getMessage());
        });
  }

  @Override
  public Future<PagedResult<RoleResponseDeleteAt>> getActiveRoles(FindAllRoles req) {
    TracingMetrics.TracingContext ctx = metrics.startSpan("RoleQueryService.getActiveRoles");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%sactive:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redis.getJson(cacheKey, PagedResult.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("role.cache_hit", true);
            @SuppressWarnings("unchecked")
            PagedResult<Role> typedCached = (PagedResult<Role>) cached;
            return Future.succeededFuture(mapRolePaginationDeleteAt(typedCached, page, pageSize));
          }
          span.setAttribute("role.cache_hit", false);
          return repo.getActiveRoles(req)
              .compose(result -> redis.setJson(cacheKey, result, CACHE_TTL).map(result))
              .map(result -> mapRolePaginationDeleteAt(result, page, pageSize));
        })
        .onSuccess(resp -> {
          span.setAttribute("roles.count", (long) resp.getData().size());
          span.setAttribute("roles.total_records", (long) resp.getTotalRecords());
          metrics.completeSpanSuccess(ctx, "get_active", "Active roles fetched successfully");
        })
        .onFailure(err -> {
          logger.error("Failed to fetch active roles", err);
          metrics.completeSpanError(ctx, "get_active", err.getMessage());
        });
  }

  @Override
  public Future<PagedResult<RoleResponseDeleteAt>> getTrashedRoles(FindAllRoles req) {
    TracingMetrics.TracingContext ctx = metrics.startSpan("RoleQueryService.getTrashedRoles");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    int page = safePage(req.getPage());
    int pageSize = safePageSize(req.getPageSize());
    String keyword = safeKeyword(req.getSearch());
    String cacheKey = String.format("%strashed:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

    return redis.getJson(cacheKey, PagedResult.class)
        .compose(cached -> {
          if (cached != null) {
            span.setAttribute("role.cache_hit", true);
            @SuppressWarnings("unchecked")
            PagedResult<Role> typedCached = (PagedResult<Role>) cached;
            return Future.succeededFuture(mapRolePaginationDeleteAt(typedCached, page, pageSize));
          }
          span.setAttribute("role.cache_hit", false);
          return repo.getTrashedRoles(req)
              .compose(result -> redis.setJson(cacheKey, result, CACHE_TTL).map(result))
              .map(result -> mapRolePaginationDeleteAt(result, page, pageSize));
        })
        .onSuccess(resp -> {
          span.setAttribute("roles.count", (long) resp.getData().size());
          span.setAttribute("roles.total_records", (long) resp.getTotalRecords());
          metrics.completeSpanSuccess(ctx, "get_trashed", "Trashed roles fetched successfully");
        })
        .onFailure(err -> {
          logger.error("Failed to fetch trashed roles", err);
          metrics.completeSpanError(ctx, "get_trashed", err.getMessage());
        });
  }

  @Override
  public Future<RoleResponse> getRoleById(Integer roleId) {
    TracingMetrics.TracingContext ctx = metrics.startSpan(
        "RoleQueryService.getRoleById",
        Attributes.builder().put("role.id", (long) roleId).build());
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

    logger.info("Fetching role by id: {}", roleId);
    String cacheKey = CACHE_PREFIX + "id:" + roleId;

    return redis.getJson(cacheKey, Role.class)
        .compose(cachedRole -> {
          if (cachedRole != null) {
            span.setAttribute("role.cache_hit", true);
            return Future.succeededFuture(RoleResponse.from(cachedRole));
          }
          span.setAttribute("role.cache_hit", false);
          return repo.getRoleById(roleId)
              .compose(role -> {
                if (role == null) {
                  return Future.<Role>failedFuture(new NotFoundException("Role not found"));
                }
                return redis.setJson(cacheKey, role, CACHE_TTL).<Role>map(v -> role);
              })
              .map(RoleResponse::from);
        })
        .onSuccess(v -> metrics.completeSpanSuccess(ctx, "get_by_id", "Role fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch role by id: {}", roleId, err);
          metrics.completeSpanError(ctx, "get_by_id", err.getMessage());
        });
  }

  @Override
  public Future<List<RoleResponse>> getRolesByUserId(Integer userId) {
    TracingMetrics.TracingContext ctx = metrics.startSpan(
        "RoleQueryService.getRolesByUserId",
        Attributes.builder().put("user.id", (long) userId).build());

    logger.info("Fetching roles for user ID: {}", userId);
    String cacheKey = CACHE_PREFIX + "user:" + userId;

    return redis.getJsonList(cacheKey, Role.class)
        .compose(cachedRoles -> {
          if (cachedRoles != null && !cachedRoles.isEmpty()) {
            return Future.succeededFuture(cachedRoles.stream().map(RoleResponse::from).toList());
          }
          return repo.getRolesByUserId(userId)
              .compose(roles -> {
                if (roles == null || roles.isEmpty()) {
                  return Future.succeededFuture(List.<Role>of());
                }
                return redis.setJsonList(cacheKey, roles, CACHE_TTL).<List<Role>>map(v -> roles);
              })
              .map(roles -> roles.stream().map(RoleResponse::from).toList());
        })
        .onSuccess(v -> metrics.completeSpanSuccess(ctx, "get_by_user_id", "Roles fetched successfully"))
        .onFailure(err -> {
          logger.error("Failed to fetch roles for user ID: {}", userId, err);
          metrics.completeSpanError(ctx, "get_by_user_id", err.getMessage());
        });
  }
}