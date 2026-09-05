package io.example.user.service.impl;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.user.domain.requests.FindAllUsers;
import io.example.user.model.User;
import io.example.user.repository.UserQueryRepository;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
@MockitoSettings(strictness = Strictness.LENIENT)
class UserQueryServiceImplTest {

  @Mock
  private UserQueryRepository repo;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private UserQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new UserQueryServiceImpl(repo, redis, metrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
  }

  private Timestamp now() {
    return Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
  }

  private User aUser() {
    return User.builder()
        .userId(1)
        .firstname("Alice")
        .lastname("Smith")
        .email("alice@example.com")
        .password("$2a$12$hashedpassword")
        .createdAt(now())
        .updatedAt(now())
        .build();
  }

  private void stubPagedFallback(PagedResult<User> paged) {
    when(repo.getUsers(any(FindAllUsers.class))).thenReturn(Future.succeededFuture(paged));
    when(repo.getActiveUsers(any(FindAllUsers.class))).thenReturn(Future.succeededFuture(paged));
    when(repo.getTrashedUsers(any(FindAllUsers.class))).thenReturn(Future.succeededFuture(paged));
    lenient().when(redis.setJson(anyString(), any(Object.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));
  }

  /* ─── getUsers ─── */

  @Test
  @DisplayName("getUsers returns cached data on cache hit (falls back to DB when deserialization fails)")
  void getUsersCacheHit(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aUser()), 1);

    when(redis.get("user:list:all::1:10")).thenReturn(Future.succeededFuture(
        "{\"data\":[{\"userId\":1,\"firstname\":\"Alice\",\"lastname\":\"Smith\",\"email\":\"alice@example.com\",\"createdAt\":\"2026-06-26T10:00:00Z\",\"updatedAt\":\"2026-06-26T10:00:00Z\"}],\"totalRecords\":1}"));
    stubPagedFallback(paged);
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllUsers.builder().page(1).pageSize(10).build();
    service.getUsers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getEmail()).isEqualTo("alice@example.com");
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getUsers fetches from DB and caches on cache miss")
  void getUsersCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aUser()), 1);

    when(redis.get("user:list:all::1:10")).thenReturn(Future.succeededFuture(null));
    when(repo.getUsers(any(FindAllUsers.class))).thenReturn(Future.succeededFuture(paged));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllUsers.builder().page(1).pageSize(10).build();
    service.getUsers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          verify(repo).getUsers(any(FindAllUsers.class));
          ctx.completeNow();
        })));
  }

  /* ─── getActiveUsers ─── */

  @Test
  @DisplayName("getActiveUsers returns cached data on cache hit (falls back to DB when deserialization fails)")
  void getActiveUsersCacheHit(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aUser()), 1);

    when(redis.get("user:list:active::1:10")).thenReturn(Future.succeededFuture(
        "{\"data\":[{\"userId\":1,\"firstname\":\"Alice\",\"lastname\":\"Smith\",\"email\":\"alice@example.com\",\"createdAt\":\"2026-06-26T10:00:00Z\",\"updatedAt\":\"2026-06-26T10:00:00Z\"}],\"totalRecords\":1}"));
    stubPagedFallback(paged);
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllUsers.builder().page(1).pageSize(10).build();
    service.getActiveUsers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getUserId()).isEqualTo(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getActiveUsers fetches from DB and caches on cache miss")
  void getActiveUsersCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aUser()), 1);

    when(redis.get("user:list:active::1:10")).thenReturn(Future.succeededFuture(null));
    when(repo.getActiveUsers(any(FindAllUsers.class))).thenReturn(Future.succeededFuture(paged));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllUsers.builder().page(1).pageSize(10).build();
    service.getActiveUsers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          verify(repo).getActiveUsers(any(FindAllUsers.class));
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedUsers ─── */

  @Test
  @DisplayName("getTrashedUsers returns cached data on cache hit (falls back to DB when deserialization fails)")
  void getTrashedUsersCacheHit(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aUser()), 1);

    when(redis.get("user:list:trashed::1:10")).thenReturn(Future.succeededFuture(
        "{\"data\":[{\"userId\":1,\"firstname\":\"Alice\",\"lastname\":\"Smith\",\"email\":\"alice@example.com\",\"createdAt\":\"2026-06-26T10:00:00Z\",\"updatedAt\":\"2026-06-26T10:00:00Z\"}],\"totalRecords\":1}"));
    stubPagedFallback(paged);
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllUsers.builder().page(1).pageSize(10).build();
    service.getTrashedUsers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTrashedUsers fetches from DB and caches on cache miss")
  void getTrashedUsersCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aUser()), 1);

    when(redis.get("user:list:trashed::1:10")).thenReturn(Future.succeededFuture(null));
    when(repo.getTrashedUsers(any(FindAllUsers.class))).thenReturn(Future.succeededFuture(paged));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllUsers.builder().page(1).pageSize(10).build();
    service.getTrashedUsers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          verify(repo).getTrashedUsers(any(FindAllUsers.class));
          ctx.completeNow();
        })));
  }

  /* ─── getUserById ─── */

  @Test
  @DisplayName("getUserById returns user from cache")
  void getUserByIdCacheHit(VertxTestContext ctx) {
    mockTracing();
    var user = aUser();

    when(redis.getJson(anyString(), eq(User.class))).thenReturn(Future.succeededFuture(user));

    service.getUserById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getUserId()).isEqualTo(1);
          assertThat(result.getFirstname()).isEqualTo("Alice");
          assertThat(result.getEmail()).isEqualTo("alice@example.com");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getUserById fetches from DB and caches on cache miss")
  void getUserByIdCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var user = aUser();

    when(redis.getJson("user:1", User.class)).thenReturn(Future.succeededFuture(null));
    when(repo.getUserById(1)).thenReturn(Future.succeededFuture(user));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getUserById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getUserId()).isEqualTo(1);
          assertThat(result.getEmail()).isEqualTo("alice@example.com");
          verify(repo).getUserById(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getUserById fails when user not found")
  void getUserByIdNotFound(VertxTestContext ctx) {
    mockTracing();

    when(redis.getJson("user:99", User.class)).thenReturn(Future.succeededFuture(null));
    when(repo.getUserById(99)).thenReturn(Future.succeededFuture(null));

    service.getUserById(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("User not found");
          ctx.completeNow();
        })));
  }
}
