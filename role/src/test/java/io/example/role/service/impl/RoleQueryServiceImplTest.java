package io.example.role.service.impl;

import io.example.common.domain.PagedResult;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.role.domain.requests.FindAllRoles;
import io.example.role.model.Role;
import io.example.role.model.RoleResponse;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RoleQueryServiceImplTest {
  @Mock private RoleQueryRepository repo; @Mock private RedisService redis; @Mock private TracingMetrics metrics;
  private RoleQueryServiceImpl service;
  @BeforeEach void setUp() { service = new RoleQueryServiceImpl(repo, redis, metrics); }
  void mt() { var c = new TracingContext(Context.root(), Instant.now()); lenient().when(metrics.startSpan(anyString())).thenReturn(c); lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(c); }
  Timestamp ts = Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
  Role aRole() { return Role.builder().roleId(1).roleName("ROLE_ADMIN").createdAt(ts).updatedAt(ts).build(); }

  @Test void getAllRoles_cacheMiss(VertxTestContext ctx) {
    mt();
    var paged = new PagedResult<>(List.of(aRole()), 1);
    when(redis.getJson(anyString(), eq(PagedResult.class))).thenReturn(Future.succeededFuture(null));
    when(repo.getRoles(any(FindAllRoles.class))).thenReturn(Future.succeededFuture(paged));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));
    service.getAllRoles(FindAllRoles.builder().page(1).pageSize(10).build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getData()).hasSize(1); ctx.completeNow(); })));
  }

  @Test void getRoleById_cacheHit(VertxTestContext ctx) {
    mt();
    when(redis.getJson("role:id:1", Role.class)).thenReturn(Future.succeededFuture(aRole()));
    service.getRoleById(1).onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r).isNotNull(); ctx.completeNow(); })));
  }

  @Test void getRoleById_cacheMiss(VertxTestContext ctx) {
    mt();
    when(redis.getJson("role:id:1", Role.class)).thenReturn(Future.succeededFuture(null));
    when(repo.getRoleById(1)).thenReturn(Future.succeededFuture(aRole()));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));
    service.getRoleById(1).onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r).isNotNull(); ctx.completeNow(); })));
  }

  @Test void getRoleById_notFound(VertxTestContext ctx) {
    mt();
    when(redis.getJson("role:id:99", Role.class)).thenReturn(Future.succeededFuture(null));
    when(repo.getRoleById(99)).thenReturn(Future.succeededFuture(null));
    service.getRoleById(99).onComplete(ctx.failing(e -> ctx.verify(() -> { assertThat(e).isInstanceOf(NotFoundException.class); ctx.completeNow(); })));
  }
}
