package io.example.user.service.impl;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.user.domain.requests.FindAllUsers;
import io.example.user.model.User;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.example.user.repository.UserQueryRepository;
import io.example.user.service.UserQueryService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {
  private static final Logger log = LoggerFactory.getLogger(UserQueryServiceImpl.class);
  private static final ObjectMapper mapper = new ObjectMapper();
  private final UserQueryRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "user:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  private PagedResult<UserResponse> mapPagination(PagedResult<User> res) {
    List<UserResponse> data = res.getData().stream().map(UserResponse::from).toList();
    return new PagedResult<>(data, res.getTotalRecords());
  }

  private PagedResult<UserResponseDeleteAt> mapPaginationDeleteAt(PagedResult<User> res) {
    List<UserResponseDeleteAt> data = res.getData().stream().map(UserResponseDeleteAt::from).toList();
    return new PagedResult<>(data, res.getTotalRecords());
  }

  @Override
  public Future<PagedResult<UserResponse>> getUsers(FindAllUsers req) {
    var ctx = metrics.startSpan("UserQueryService.getUsers");
    String cacheKey = CACHE_PREFIX + "list:all:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
        + req.getPage() + ":" + req.getPageSize();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              PagedResult<User> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<User>>() {});
              return Future.succeededFuture(mapPagination(typedCached));
            } catch (Exception e) {
              log.warn("Failed to deserialize cached users: {}", e.getMessage());
            }
          }
          return repository.getUsers(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
              .map(this::mapPagination);
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getUsers", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getUsers", e.getMessage()));
  }

  @Override
  public Future<PagedResult<UserResponseDeleteAt>> getActiveUsers(FindAllUsers req) {
    var ctx = metrics.startSpan("UserQueryService.getActiveUsers");
    String cacheKey = CACHE_PREFIX + "list:active:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
        + req.getPage() + ":" + req.getPageSize();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              PagedResult<User> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<User>>() {});
              return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
            } catch (Exception e) {
              log.warn("Failed to deserialize cached active users: {}", e.getMessage());
            }
          }
          return repository.getActiveUsers(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
              .map(this::mapPaginationDeleteAt);
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getActiveUsers", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getActiveUsers", e.getMessage()));
  }

  @Override
  public Future<PagedResult<UserResponseDeleteAt>> getTrashedUsers(FindAllUsers req) {
    var ctx = metrics.startSpan("UserQueryService.getTrashedUsers");
    String cacheKey = CACHE_PREFIX + "list:trashed:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
        + req.getPage() + ":" + req.getPageSize();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              PagedResult<User> typedCached = mapper.readValue(jsonStr, new TypeReference<PagedResult<User>>() {});
              return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
            } catch (Exception e) {
              log.warn("Failed to deserialize cached trashed users: {}", e.getMessage());
            }
          }
          return repository.getTrashedUsers(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
              .map(this::mapPaginationDeleteAt);
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTrashedUsers", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getTrashedUsers", e.getMessage()));
  }

  @Override
  public Future<UserResponse> getUserById(Integer id) {
    var ctx = metrics.startSpan("UserQueryService.getUserById", Attributes.builder().put("user.id", (long) id).build());
    String key = CACHE_PREFIX + id;

    return redis.getJson(key, User.class)
        .compose(cached -> {
          if (cached != null) {
            return Future.succeededFuture(UserResponse.from(cached));
          }
          return repository.getUserById(id)
              .compose(db -> {
                if (db == null) {
                  return Future.<User>failedFuture(new NotFoundException("User not found"));
                }
                return redis.setJson(key, db, CACHE_TTL).<User>map(v -> db);
              })
              .map(UserResponse::from);
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getUserById", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getUserById", e.getMessage()));
  }
}