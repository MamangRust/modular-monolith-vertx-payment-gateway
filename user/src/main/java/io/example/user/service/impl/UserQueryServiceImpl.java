package io.example.user.service.impl;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.common.model.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.user.model.User;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.example.user.repository.UserQueryRepository;
import io.example.user.service.UserQueryService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import pb.user.User.FindAllUserRequest;
import pb.user.User.FindByIdUserRequest;

public class UserQueryServiceImpl implements UserQueryService {
  private static final Logger log = LoggerFactory.getLogger(UserQueryServiceImpl.class);
  private final UserQueryRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "user:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  public UserQueryServiceImpl(UserQueryRepository repository, RedisService redis, TracingMetrics metrics) {
    this.repository = repository;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<ApiResponsePagination<List<UserResponse>>> getUsers(FindAllUserRequest req) {
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String search = req.getSearch();

    String cacheKey = CACHE_PREFIX + "list:all:" + (search != null ? search : "") + ":" + page + ":" + pageSize;
    var ctx = metrics.startSpan("UserQueryService.getUsers");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            JsonObject json = new JsonObject(cached);
            List<UserResponse> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(UserResponse.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            metrics.completeSpanSuccess(ctx, "getUsers", "Success (from cache)");
            return Future.succeededFuture(new ApiResponsePagination<>("success", "Users fetched successfully (from cache)", data, meta));
          }
          return repository.getUsers(search, page, pageSize)
              .map(res -> {
                ApiResponsePagination<List<UserResponse>> response = mapPagination(res, page, pageSize);
                redis.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL);
                return response;
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getUsers", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getUsers", e.getMessage()))
        .recover(e -> Future.succeededFuture(ApiResponsePagination.error(e.getMessage())));
  }

  @Override
  public Future<ApiResponsePagination<List<UserResponseDeleteAt>>> getActiveUsers(FindAllUserRequest req) {
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String search = req.getSearch();

    String cacheKey = CACHE_PREFIX + "list:active:" + (search != null ? search : "") + ":" + page + ":" + pageSize;
    var ctx = metrics.startSpan("UserQueryService.getActiveUsers");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            JsonObject json = new JsonObject(cached);
            List<UserResponseDeleteAt> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(UserResponseDeleteAt.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            metrics.completeSpanSuccess(ctx, "getActiveUsers", "Success (from cache)");
            return Future.succeededFuture(new ApiResponsePagination<>("success", "Active users fetched successfully (from cache)", data, meta));
          }
          return repository.getActiveUsers(search, page, pageSize)
              .map(res -> {
                ApiResponsePagination<List<UserResponseDeleteAt>> response = mapPaginationDeleteAt(res, page, pageSize);
                redis.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL);
                return response;
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getActiveUsers", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getActiveUsers", e.getMessage()))
        .recover(e -> Future.succeededFuture(ApiResponsePagination.error(e.getMessage())));
  }

  @Override
  public Future<ApiResponsePagination<List<UserResponseDeleteAt>>> getTrashedUsers(FindAllUserRequest req) {
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String search = req.getSearch();

    String cacheKey = CACHE_PREFIX + "list:trashed:" + (search != null ? search : "") + ":" + page + ":" + pageSize;
    var ctx = metrics.startSpan("UserQueryService.getTrashedUsers");

    return redis.get(cacheKey)
        .compose(cached -> {
          if (cached != null) {
            JsonObject json = new JsonObject(cached);
            List<UserResponseDeleteAt> data = json.getJsonArray("data").stream()
                .map(o -> ((JsonObject) o).mapTo(UserResponseDeleteAt.class)).toList();
            PaginationMeta meta = json.getJsonObject("pagination").mapTo(PaginationMeta.class);
            metrics.completeSpanSuccess(ctx, "getTrashedUsers", "Success (from cache)");
            return Future.succeededFuture(new ApiResponsePagination<>("success", "Trashed users fetched successfully (from cache)", data, meta));
          }
          return repository.getTrashedUsers(search, page, pageSize)
              .map(res -> {
                ApiResponsePagination<List<UserResponseDeleteAt>> response = mapPaginationDeleteAt(res, page, pageSize);
                redis.setJson(cacheKey, JsonObject.mapFrom(response), CACHE_TTL);
                return response;
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTrashedUsers", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getTrashedUsers", e.getMessage()))
        .recover(e -> Future.succeededFuture(ApiResponsePagination.error(e.getMessage())));
  }

  @Override
  public Future<ApiResponse<UserResponse>> getUserById(FindByIdUserRequest req) {
    Integer id = req.getId();
    var ctx = metrics.startSpan("UserQueryService.getUserById", Attributes.builder().put("user.id", (long) id).build());
    String key = CACHE_PREFIX + id;

    return redis.get(key)
        .compose(cached -> {
          if (cached != null && !cached.isEmpty()) {
            try {
              User user = User.fromJson(new JsonObject(cached));
              metrics.completeSpanSuccess(ctx, "getUserById", "Success (from cache)");
              return Future.succeededFuture(
                  ApiResponse.success("User fetched successfully (from cache)", UserResponse.from(user)));
            } catch (Exception ex) {
              log.warn("Failed parsing cached user {}", id, ex);
            }
          }
          return repository.getUserById(id)
              .compose(db -> {
                if (db == null)
                  return Future.failedFuture("User not found");
                redis.setJson(key, db.toJson(), CACHE_TTL);
                return Future.succeededFuture(ApiResponse.success("User fetched successfully", UserResponse.from(db)));
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getUserById", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getUserById", e.getMessage()))
        .recover(e -> Future.succeededFuture(ApiResponse.error(e.getMessage())));
  }

  private ApiResponsePagination<List<UserResponse>> mapPagination(io.example.common.domain.PagedResult<User> res,
      int page, int pageSize) {
    int totalRecords = res.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<UserResponse> data = res.getData().stream().map(UserResponse::from).toList();
    return ApiResponsePagination.success("Users fetched successfully", data,
        new PaginationMeta(page, pageSize, totalPages, totalRecords));
  }

  private ApiResponsePagination<List<UserResponseDeleteAt>> mapPaginationDeleteAt(
      io.example.common.domain.PagedResult<User> res, int page, int pageSize) {
    int totalRecords = res.getTotalRecords();
    int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    List<UserResponseDeleteAt> data = res.getData().stream().map(UserResponseDeleteAt::from).toList();
    return ApiResponsePagination.success("Users fetched successfully", data,
        new PaginationMeta(page, pageSize, totalPages, totalRecords));
  }
}
