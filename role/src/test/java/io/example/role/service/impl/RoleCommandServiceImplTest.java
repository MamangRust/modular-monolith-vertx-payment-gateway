package io.example.role.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.role.domain.requests.CreateRoleRequest;
import io.example.role.domain.requests.UpdateRoleRequest;
import io.example.role.model.Role;
import io.example.role.repository.RoleCommandRepository;
import io.example.role.repository.RoleQueryRepository;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class RoleCommandServiceImplTest {
  @Mock private RoleCommandRepository repo; @Mock private RoleQueryRepository queryRepo;
  @Mock private RedisService redis; @Mock private TracingMetrics metrics;
  private RoleCommandServiceImpl service;

  @BeforeEach void setUp() { service = new RoleCommandServiceImpl(repo, queryRepo, redis, metrics); }
  void mt() { var c = new TracingContext(Context.root(), Instant.now()); lenient().when(metrics.startSpan(anyString())).thenReturn(c); lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(c); }
  Timestamp ts = Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
  Role aRole() { return Role.builder().roleId(1).roleName("ROLE_ADMIN").createdAt(ts).updatedAt(ts).build(); }
  Role aTrashed() { return Role.builder().roleId(1).roleName("ROLE_ADMIN").createdAt(ts).updatedAt(ts).deletedAt(ts).build(); }

  @Test void createRole(VertxTestContext ctx) {
    mt();
    when(repo.createRole(any(CreateRoleRequest.class))).thenReturn(Future.succeededFuture(aRole()));
    service.createRole(CreateRoleRequest.builder().name("ROLE_ADMIN").build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r).isNotNull(); ctx.completeNow(); })));
  }

  @Test void updateRole(VertxTestContext ctx) {
    mt();
    when(repo.updateRole(any(UpdateRoleRequest.class))).thenReturn(Future.succeededFuture(aRole()));
    lenient().when(redis.delete(anyString())).thenReturn(Future.succeededFuture(1L));
    lenient().when(redis.delete(anyString())).thenReturn(Future.succeededFuture(1L));
    service.updateRole(UpdateRoleRequest.builder().roleId(1).name("ROLE_USER").build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r).isNotNull(); ctx.completeNow(); })));
  }

  @Test void updateRole_notFound(VertxTestContext ctx) {
    mt();
    when(repo.updateRole(any(UpdateRoleRequest.class))).thenReturn(Future.succeededFuture(null));
    service.updateRole(UpdateRoleRequest.builder().roleId(99).name("GHOST").build())
        .onComplete(ctx.failing(e -> ctx.verify(() -> { assertThat(e).isInstanceOf(NotFoundException.class); ctx.completeNow(); })));
  }

  @Test void trashRole(VertxTestContext ctx) {
    mt();
    when(repo.trashed(1)).thenReturn(Future.succeededFuture(aTrashed()));
    lenient().when(redis.delete(anyString())).thenReturn(Future.succeededFuture(1L));
    lenient().when(redis.delete(anyString())).thenReturn(Future.succeededFuture(1L));
    service.trashRole(1).onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r).isNotNull(); ctx.completeNow(); })));
  }

  @Test void trashRole_notFound(VertxTestContext ctx) {
    mt();
    when(repo.trashed(1)).thenReturn(Future.succeededFuture(null));
    service.trashRole(1).onComplete(ctx.failing(e -> ctx.verify(() -> { assertThat(e).isInstanceOf(NotFoundException.class); ctx.completeNow(); })));
  }

  @Test void restoreRole(VertxTestContext ctx) {
    mt();
    when(queryRepo.findByTrashedId(1)).thenReturn(Future.succeededFuture(aTrashed()));
    when(repo.restore(1)).thenReturn(Future.succeededFuture(aRole()));
    lenient().when(redis.delete(anyString())).thenReturn(Future.succeededFuture(1L));
    lenient().when(redis.delete(anyString())).thenReturn(Future.succeededFuture(1L));
    service.restoreRole(1).onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r).isNotNull(); ctx.completeNow(); })));
  }

  @Test void restoreRole_notTrashed(VertxTestContext ctx) {
    mt();
    when(queryRepo.findByTrashedId(1)).thenReturn(Future.succeededFuture(null));
    service.restoreRole(1).onComplete(ctx.failing(e -> ctx.verify(() -> { assertThat(e).isInstanceOf(BadRequestException.class); ctx.completeNow(); })));
  }

  @Test void deletePermanent(VertxTestContext ctx) {
    mt();
    when(queryRepo.findByTrashedId(1)).thenReturn(Future.succeededFuture(aTrashed()));
    when(repo.deletePermanent(1)).thenReturn(Future.succeededFuture(true));
    lenient().when(redis.delete(anyString())).thenReturn(Future.succeededFuture(1L));
    service.deletePermanent(1).onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }

  @Test void restoreAllRoles(VertxTestContext ctx) {
    mt();
    when(repo.restoreAllRoles()).thenReturn(Future.succeededFuture(3));
    lenient().when(redis.delete(anyString())).thenReturn(Future.succeededFuture(1L));
    service.restoreAllRoles().onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }

  @Test void restoreAllRoles_none(VertxTestContext ctx) {
    mt();
    when(repo.restoreAllRoles()).thenReturn(Future.succeededFuture(0));
    service.restoreAllRoles().onComplete(ctx.failing(e -> ctx.verify(() -> { assertThat(e).isInstanceOf(NotFoundException.class); ctx.completeNow(); })));
  }

  @Test void deleteAllPermanentRoles(VertxTestContext ctx) {
    mt();
    when(repo.deleteAllPermanentRoles()).thenReturn(Future.succeededFuture(2));
    lenient().when(redis.delete(anyString())).thenReturn(Future.succeededFuture(1L));
    service.deleteAllPermanentRoles().onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }
}
