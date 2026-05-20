package io.example.role.service.impl;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.role.model.Role;
import io.example.role.model.RoleResponse;
import io.example.role.model.RoleResponseDeleteAt;
import io.example.role.repository.RoleCommandRepository;
import io.example.role.service.RoleCommandService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import pb.role.RoleCommand.CreateRoleRequest;
import pb.role.RoleCommand.UpdateRoleRequest;

public class RoleCommandServiceImpl implements RoleCommandService {
  private static final Logger logger = LoggerFactory.getLogger(RoleCommandServiceImpl.class);

  private final RoleCommandRepository repo;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "role:";

  public RoleCommandServiceImpl(
      RoleCommandRepository repo,
      RedisService redis,
      TracingMetrics metrics) {
    this.repo = repo;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<ApiResponse<RoleResponse>> createRole(CreateRoleRequest req) {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan(
        "RoleCommandService.createRole",
        Attributes.builder()
            .put("role.name", Objects.requireNonNull(req.getName()))
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Creating role: {}", req.getName());

    return repo.createRole(req.getName())
        .map(created -> {
          span.setAttribute("role.id", (long) created.getRoleId());
          metrics.completeSpanSuccess(tracingContext, "create", "Role created successfully");
          return ApiResponse.success(
              "Role created successfully",
              RoleResponse.from(created));
        })
        .recover(err -> {
          logger.error("Failed to create role: {}", req.getName(), err);
          metrics.completeSpanError(tracingContext, "create", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<RoleResponse>error("Failed to create role: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<RoleResponse>> updateRole(UpdateRoleRequest req) {
    Integer roleId = req.getId();
    TracingMetrics.TracingContext tracingContext = metrics.startSpan(
        "RoleCommandService.updateRole",
        Attributes.builder()
            .put("role.id", (long) roleId)
            .put("role.name", Objects.requireNonNull(req.getName()))
            .build());

    logger.info("Updating role: {}, name: {}", roleId, req.getName());

    return repo.updateRole(roleId, req.getName())
        .compose((Role updatedRole) -> {
          if (updatedRole == null) {
            return Future.failedFuture(new NotFoundException("Role not found"));
          }
          String cacheKey = CACHE_PREFIX + "id:" + roleId;
          return redis.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Role {} cache invalidated", roleId);
                }
              })
              .onFailure(err -> logger.warn("Failed to invalidate cache for role {}: {}", roleId, err.getMessage()))
              .map(updatedRole);
        })
        .map((Role updatedRole) -> {
          metrics.completeSpanSuccess(tracingContext, "update", "Role updated successfully");
          return ApiResponse.success(
              "Role updated successfully",
              RoleResponse.from(updatedRole));
        })
        .recover(err -> {
          logger.error("Failed to update role: {}", roleId, err);
          metrics.completeSpanError(tracingContext, "update", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<RoleResponse>error("Failed to update role: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<RoleResponseDeleteAt>> trashRole(Integer roleId) {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan(
        "RoleCommandService.trashed",
        Attributes.builder()
            .put("role.id", (long) roleId)
            .build());

    logger.info("Trashing role: {}", roleId);

    return repo.trashed(roleId)
        .compose(role -> {
          if (role == null) {
            return Future.failedFuture(new NotFoundException("Role not found with id: " + roleId));
          }
          String cacheKey = CACHE_PREFIX + "id:" + roleId;
          return redis.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Role {} cache invalidated on trash", roleId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for trashed role {}: {}", roleId, err.getMessage()))
              .map(role);
        })
        .map(role -> {
          metrics.completeSpanSuccess(tracingContext, "trashed", "Role trashed successfully");
          return ApiResponse.success("Role trashed successfully", RoleResponseDeleteAt.from(role));
        })
        .recover(err -> {
          logger.error("Failed to trash role: {}", roleId, err);
          metrics.completeSpanError(tracingContext, "trashed", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<RoleResponseDeleteAt>error("Failed to trash role: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<RoleResponseDeleteAt>> restoreRole(Integer roleId) {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan(
        "RoleCommandService.restore",
        Attributes.builder()
            .put("role.id", (long) roleId)
            .build());

    logger.info("Restoring role: {}", roleId);

    return repo.restore(roleId)
        .compose(role -> {
          if (role == null) {
            return Future.failedFuture(new NotFoundException("Role not found with id: " + roleId));
          }
          String cacheKey = CACHE_PREFIX + "id:" + roleId;
          return redis.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Role {} cache invalidated on restore", roleId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for restored role {}: {}", roleId, err.getMessage()))
              .map(role);
        })
        .map(role -> {
          metrics.completeSpanSuccess(tracingContext, "restore", "Role restored successfully");
          return ApiResponse.success(
              "Role restored successfully",
              RoleResponseDeleteAt.from(role));
        })
        .recover(err -> {
          logger.error("Failed to restore role: {}", roleId, err);
          metrics.completeSpanError(tracingContext, "restore", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<RoleResponseDeleteAt>error("Failed to restore role: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> deletePermanent(Integer roleId) {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan(
        "RoleCommandService.deletePermanent",
        Attributes.builder()
            .put("role.id", (long) roleId)
            .build());

    logger.info("Permanently deleting role: {}", roleId);

    return repo.deletePermanent(roleId)
        .compose(v -> {
          String cacheKey = CACHE_PREFIX + "id:" + roleId;
          return redis.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Role {} cache invalidated on permanent delete", roleId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for deleted role {}: {}", roleId, err.getMessage()))
              .map(v);
        })
        .map(v -> {
          logger.info("Role deleted successfully: {}", roleId);
          metrics.completeSpanSuccess(tracingContext, "deletePermanent", "Role deleted permanently");
          return ApiResponse.<Void>success("success", null);
        })
        .recover(throwable -> {
          logger.error("Failed to deletePermanent role: {}", roleId, throwable);
          metrics.completeSpanError(tracingContext, "deletePermanent", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponse.<Void>error("Failed to delete role: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> restoreAllRoles() {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan("RoleService.restoreAll");

    logger.info("Attempting to restore all trashed roles");

    return repo.restoreAllRoles()
        .compose(v -> {
          logger.info("All roles restored successfully");
          metrics.completeSpanSuccess(
              tracingContext,
              "restore_all",
              "All roles restored");
          return Future.succeededFuture(
              ApiResponse.<Void>success("All roles restored successfully"));
        })
        .recover(throwable -> {
          logger.error("Failed to restore all roles", throwable);
          metrics.completeSpanError(
              tracingContext,
              "restore_all",
              throwable.getMessage());
          return Future.succeededFuture(
              ApiResponse.<Void>error(
                  "Failed to restore all roles: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> deleteAllPermanentRoles() {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan("RoleService.deleteAllPermanent");

    logger.info("Attempting to permanently delete all trashed roles");

    return repo.deleteAllPermanentRoles()
        .compose(v -> {
          logger.info("All trashed roles permanently deleted");
          metrics.completeSpanSuccess(
              tracingContext,
              "deleteAllPermanent",
              "All roles permanently deleted");
          return Future.succeededFuture(
              ApiResponse.<Void>success("All roles permanently deleted"));
        })
        .recover(throwable -> {
          logger.error("Failed to permanently delete all roles", throwable);
          metrics.completeSpanError(
              tracingContext,
              "deleteAllPermanent",
              throwable.getMessage());
          return Future.succeededFuture(
              ApiResponse.<Void>error(
                  "Failed to permanently delete all roles: " + throwable.getMessage()));
        });
  }
}
