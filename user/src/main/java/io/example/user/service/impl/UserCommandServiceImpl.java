package io.example.user.service.impl;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.example.common.model.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.example.user.repository.UserCommandRepository;
import io.example.user.service.UserCommandService;
import io.vertx.core.Future;
import pb.user.User.FindByIdUserRequest;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;

public class UserCommandServiceImpl implements UserCommandService {
  private final UserCommandRepository repository;
  private final RedisService redis;
  private final TracingMetrics metrics;

  public UserCommandServiceImpl(UserCommandRepository repository, RedisService redis, TracingMetrics metrics) {
    this.repository = repository;
    this.redis = redis;
    this.metrics = metrics;
  }

  private String hash(String raw) {
    return BCrypt.withDefaults().hashToString(12, raw.toCharArray());
  }

  private Future<Void> evict(Integer id) {
    return redis.delete("user:" + id).mapEmpty();
  }

  @Override
  public Future<ApiResponse<UserResponse>> createUser(CreateUserRequest req) {
    var ctx = metrics.startSpan("UserCommandService.createUser");
    if (!req.getPassword().equals(req.getConfirmPassword())) {
      return Future.succeededFuture(ApiResponse.error("Passwords do not match with confirmation"));
    }
    return repository.createUser(req.getFirstname(), req.getLastname(), req.getEmail(), hash(req.getPassword()))
        .compose(user -> repository.assignDefaultAdminRole(user.getUserId()).map(user))
        .map(u -> ApiResponse.success("User created successfully", UserResponse.from(u)))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "createUser", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "createUser", e.getMessage()))
        .recover(e -> Future.succeededFuture(ApiResponse.error(e.getMessage())));
  }

  @Override
  public Future<ApiResponse<UserResponse>> updateUser(UpdateUserRequest req) {
    var ctx = metrics.startSpan("UserCommandService.updateUser");
    Integer id = req.getId();

    Future<io.example.user.model.User> updateOp;
    if (!req.getPassword().isEmpty()) {
      if (!req.getPassword().equals(req.getConfirmPassword())) {
        return Future.succeededFuture(ApiResponse.error("Passwords do not match with confirmation"));
      }
      updateOp = repository.updateUser(id, req.getFirstname(), req.getLastname(), req.getEmail())
          .compose(u -> {
            if (u == null)
              return Future.failedFuture("User not found");
            return repository.updatePassword(id, hash(req.getPassword()));
          });
    } else {
      updateOp = repository.updateUser(id, req.getFirstname(), req.getLastname(), req.getEmail());
    }

    return updateOp
        .compose(r -> {
          if (r == null)
            return Future.failedFuture("User not found");
          return evict(id).map(r);
        })
        .map(u -> ApiResponse.success("User updated successfully", UserResponse.from(u)))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "updateUser", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "updateUser", e.getMessage()))
        .recover(e -> Future.succeededFuture(ApiResponse.error(e.getMessage())));
  }

  @Override
  public Future<ApiResponse<UserResponseDeleteAt>> trashUser(FindByIdUserRequest req) {
    Integer id = req.getId();
    var ctx = metrics.startSpan("UserCommandService.trashUser");
    return repository.trashed(id)
        .compose(r -> {
          if (r == null)
            return Future.failedFuture("User not found");
          return evict(id).map(r);
        })
        .map(u -> ApiResponse.success("User trashed successfully", UserResponseDeleteAt.from(u)))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "trashUser", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "trashUser", e.getMessage()))
        .recover(e -> Future.succeededFuture(ApiResponse.error(e.getMessage())));
  }

  @Override
  public Future<ApiResponse<UserResponseDeleteAt>> restoreUser(FindByIdUserRequest req) {
    Integer id = req.getId();
    var ctx = metrics.startSpan("UserCommandService.restoreUser");
    return repository.restore(id)
        .compose(r -> {
          if (r == null)
            return Future.failedFuture("User not found");
          return evict(id).map(r);
        })
        .map(u -> ApiResponse.success("User restored successfully", UserResponseDeleteAt.from(u)))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "restoreUser", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "restoreUser", e.getMessage()))
        .recover(e -> Future.succeededFuture(ApiResponse.error(e.getMessage())));
  }

  @Override
  public Future<ApiResponse<Void>> deletePermanent(FindByIdUserRequest req) {
    Integer id = req.getId();
    var ctx = metrics.startSpan("UserCommandService.deletePermanent");
    return repository.deletePermanent(id)
        .compose(v -> evict(id))
        .map(v -> ApiResponse.<Void>success("User permanently deleted successfully"))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "deletePermanent", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "deletePermanent", e.getMessage()))
        .recover(e -> Future.succeededFuture(ApiResponse.<Void>error(e.getMessage())));
  }

  @Override
  public Future<ApiResponse<Void>> restoreAllUsers() {
    var ctx = metrics.startSpan("UserCommandService.restoreAllUsers");
    return repository.restoreAllUsers()
        .map(v -> ApiResponse.<Void>success("All users restored successfully"))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "restoreAllUsers", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "restoreAllUsers", e.getMessage()))
        .recover(e -> Future.succeededFuture(ApiResponse.<Void>error(e.getMessage())));
  }

  @Override
  public Future<ApiResponse<Void>> deleteAllPermanentUsers() {
    var ctx = metrics.startSpan("UserCommandService.deleteAllPermanentUsers");
    return repository.deleteAllPermanentUsers()
        .map(v -> ApiResponse.<Void>success("All trashed users permanently deleted successfully"))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "deleteAllPermanentUsers", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "deleteAllPermanentUsers", e.getMessage()))
        .recover(e -> Future.succeededFuture(ApiResponse.<Void>error(e.getMessage())));
  }
}
