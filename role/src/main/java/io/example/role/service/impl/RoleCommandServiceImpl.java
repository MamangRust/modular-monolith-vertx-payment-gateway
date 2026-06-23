package io.example.role.service.impl;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.role.domain.requests.CreateRoleRequest;
import io.example.role.domain.requests.UpdateRoleRequest;
import io.example.role.model.Role;
import io.example.role.model.RoleResponse;
import io.example.role.model.RoleResponseDeleteAt;
import io.example.role.repository.RoleCommandRepository;
import io.example.role.repository.RoleQueryRepository;
import io.example.role.service.RoleCommandService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoleCommandServiceImpl implements RoleCommandService {
  private static final Logger logger = LoggerFactory.getLogger(RoleCommandServiceImpl.class);
  private static final String CACHE_PREFIX = "role:";

  private final RoleCommandRepository repo;
  private final RoleQueryRepository queryRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private Future<Void> invalidateCache(Integer roleId) {
    return redis.delete(CACHE_PREFIX + "id:" + roleId)
        .compose(v -> redis.delete(CACHE_PREFIX + "list:*"))
        .<Void>mapEmpty();
  }

  private Future<Void> invalidateListCache() {
    return redis.delete(CACHE_PREFIX + "list:*").<Void>mapEmpty();
  }

  @Override
  public Future<RoleResponse> createRole(CreateRoleRequest req) {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan(
        "RoleCommandService.createRole",
        Attributes.builder()
            .put("role.name", Objects.requireNonNull(req.getName()))
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Creating role: {}", req.getName());

    return repo.createRole(req)
        .map(created -> {
          span.setAttribute("role.id", (long) created.getRoleId());
          return RoleResponse.from(created);
        })
        .onSuccess(v -> metrics.completeSpanSuccess(tracingContext, "create", "Role created successfully"))
        .onFailure(err -> {
          logger.error("Failed to create role: {}", req.getName(), err);
          metrics.completeSpanError(tracingContext, "create", err.getMessage());
        });
  }

  @Override
  public Future<RoleResponse> updateRole(UpdateRoleRequest req) {
    Integer roleId = req.getRoleId();
    TracingMetrics.TracingContext tracingContext = metrics.startSpan(
        "RoleCommandService.updateRole",
        Attributes.builder()
            .put("role.id", (long) roleId)
            .put("role.name", Objects.requireNonNull(req.getName()))
            .build());

    logger.info("Updating role: {}, name: {}", roleId, req.getName());

    return repo.updateRole(req)
        .compose(updatedRole -> {
          if (updatedRole == null) {
            return Future.<Role>failedFuture(new NotFoundException("Role not found"));
          }
          return invalidateCache(roleId).<Role>map(v -> updatedRole);
        })
        .map(RoleResponse::from)
        .onSuccess(v -> metrics.completeSpanSuccess(tracingContext, "update", "Role updated successfully"))
        .onFailure(err -> {
          logger.error("Failed to update role: {}", roleId, err);
          metrics.completeSpanError(tracingContext, "update", err.getMessage());
        });
  }

  @Override
  public Future<RoleResponseDeleteAt> trashRole(Integer roleId) {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan(
        "RoleCommandService.trashed",
        Attributes.builder().put("role.id", (long) roleId).build());

    return repo.trashed(roleId)
        .compose(role -> {
          if (role == null) {
            return Future.<Role>failedFuture(new NotFoundException("Role not found with id: " + roleId));
          }
          return invalidateCache(roleId).<Role>map(v -> role);
        })
        .map(RoleResponseDeleteAt::from)
        .onSuccess(v -> metrics.completeSpanSuccess(tracingContext, "trashed", "Role trashed successfully"))
        .onFailure(err -> {
          logger.error("Failed to trash role: {}", roleId, err);
          metrics.completeSpanError(tracingContext, "trashed", err.getMessage());
        });
  }

  @Override
  public Future<RoleResponseDeleteAt> restoreRole(Integer roleId) {
    var ctx = metrics.startSpan("RoleCommandService.restore",
        Attributes.builder().put("role.id", (long) roleId).build());

    return queryRepository.findByTrashedId(roleId)
        .compose((Role trashed) -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Role not found or must be trashed first"));
          return repo.restore(roleId);
        })
        .compose((Role restored) -> {
          if (restored == null)
            return Future.failedFuture(new NotFoundException("Role not found on restore"));
          return invalidateCache(roleId).map(v -> restored);
        })
        .map(RoleResponseDeleteAt::from)
        .onSuccess(v -> metrics.completeSpanSuccess(ctx, "restore", "Role restored successfully"))
        .onFailure(err -> {
          logger.error("Failed to restore role: {}", roleId, err);
          metrics.completeSpanError(ctx, "restore", err.getMessage());
        });
  }

  @Override
  public Future<Void> deletePermanent(Integer roleId) {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan(
        "RoleCommandService.deletePermanent",
        Attributes.builder().put("role.id", (long) roleId).build());

    return queryRepository.findByTrashedId(roleId).compose(role -> {
      if (role == null) {
        return Future.<Void>failedFuture(new NotFoundException("Role not found with id: " + roleId));
      }

      return repo.deletePermanent(roleId)
          .compose(v -> invalidateCache(roleId))
          .onSuccess(v -> metrics.completeSpanSuccess(tracingContext, "deletePermanent", "Role deleted permanently"))
          .onFailure(err -> {
            logger.error("Failed to deletePermanent role: {}", roleId, err);
            metrics.completeSpanError(tracingContext, "deletePermanent", err.getMessage());
          });
    });
  }

  @Override
  public Future<Void> restoreAllRoles() {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan("RoleService.restoreAll");

    return repo.restoreAllRoles()
        .compose(v -> {
          if (v == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No roles found to restore"));
          }

          return invalidateListCache();
        })
        .onSuccess(v -> metrics.completeSpanSuccess(tracingContext, "restore_all", "All roles restored"))
        .onFailure(err -> {
          logger.error("Failed to restore all roles", err);
          metrics.completeSpanError(tracingContext, "restore_all", err.getMessage());
        });
  }

  @Override
  public Future<Void> deleteAllPermanentRoles() {
    TracingMetrics.TracingContext tracingContext = metrics.startSpan("RoleService.deleteAllPermanent");

    return repo.deleteAllPermanentRoles()
        .compose(v -> {
          if (v == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No roles found to delete permanently"));
          }

          return invalidateListCache();
        })
        .onSuccess(
            v -> metrics.completeSpanSuccess(tracingContext, "deleteAllPermanent", "All roles permanently deleted"))
        .onFailure(err -> {
          logger.error("Failed to permanently delete all roles", err);
          metrics.completeSpanError(tracingContext, "deleteAllPermanent", err.getMessage());
        });
  }
}