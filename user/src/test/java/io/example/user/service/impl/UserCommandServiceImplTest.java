package io.example.user.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.user.domain.requests.CreateUserRequest;
import io.example.user.domain.requests.UpdateUserRequest;
import io.example.user.model.User;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.example.user.repository.UserCommandRepository;
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
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class UserCommandServiceImplTest {

  @Mock
  private UserCommandRepository repo;

  @Mock
  private UserQueryRepository repoQuery;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics tracingMetrics;

  private UserCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new UserCommandServiceImpl(repo, repoQuery, redisService, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
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

  private void stubDelete() {
    when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
  }

  /* ─── createUser ─── */

  @Test
  @DisplayName("createUser creates user and assigns default admin role")
  void createUserSuccess(VertxTestContext ctx) {
    mockTracing();
    var user = aUser();

    when(repo.createUser(any(CreateUserRequest.class))).thenReturn(Future.succeededFuture(user));
    when(repo.assignDefaultAdminRole(1)).thenReturn(Future.succeededFuture());

    var req = CreateUserRequest.builder()
        .firstName("Alice").lastName("Smith").email("alice@example.com")
        .password("Secret123!").confirmPassword("Secret123!")
        .build();

    service.createUser(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getUserId()).isEqualTo(1);
          assertThat(result.getFirstname()).isEqualTo("Alice");
          assertThat(result.getEmail()).isEqualTo("alice@example.com");
          verify(repo).createUser(any(CreateUserRequest.class));
          verify(repo).assignDefaultAdminRole(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createUser fails when passwords do not match")
  void createUserPasswordMismatch(VertxTestContext ctx) {
    mockTracing();

    var req = CreateUserRequest.builder()
        .firstName("Alice").lastName("Smith").email("alice@example.com")
        .password("Secret123!").confirmPassword("DifferentPassword!")
        .build();

    service.createUser(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("Passwords do not match with confirmation");
          ctx.completeNow();
        })));
  }

  /* ─── updateUser (no password change) ─── */

  @Test
  @DisplayName("updateUser updates user and evicts cache when no password change")
  void updateUserNoPasswordSuccess(VertxTestContext ctx) {
    mockTracing();
    var user = aUser();

    when(repo.updateUser(any(UpdateUserRequest.class))).thenReturn(Future.succeededFuture(user));
    stubDelete();

    var req = UpdateUserRequest.builder()
        .userId(1).firstName("Alice").lastName("Johnson").email("alice@example.com")
        .build();

    service.updateUser(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getUserId()).isEqualTo(1);
          assertThat(result.getLastname()).isEqualTo("Smith");
          verify(repo).updateUser(any(UpdateUserRequest.class));
          verify(redisService).delete("user:1");
          ctx.completeNow();
        })));
  }

  /* ─── updateUser (with password change) ─── */

  @Test
  @DisplayName("updateUser updates user and password then evicts cache")
  void updateUserWithPasswordSuccess(VertxTestContext ctx) {
    mockTracing();
    var user = aUser();

    when(repo.updateUser(any(UpdateUserRequest.class))).thenReturn(Future.succeededFuture(user));
    when(repo.updatePassword(any())).thenReturn(Future.succeededFuture(user));
    stubDelete();

    var req = UpdateUserRequest.builder()
        .userId(1).firstName("Alice").lastName("Smith").email("alice@example.com")
        .password("NewPass123!").confirmPassword("NewPass123!")
        .build();

    service.updateUser(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getUserId()).isEqualTo(1);
          verify(repo).updateUser(any(UpdateUserRequest.class));
          verify(repo).updatePassword(any());
          verify(redisService).delete("user:1");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateUser fails when passwords do not match")
  void updateUserPasswordMismatch(VertxTestContext ctx) {
    mockTracing();

    var req = UpdateUserRequest.builder()
        .userId(1).firstName("Alice").lastName("Smith").email("alice@example.com")
        .password("NewPass123!").confirmPassword("Mismatch!")
        .build();

    service.updateUser(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("Passwords do not match with confirmation");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateUser fails when user not found")
  void updateUserNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.updateUser(any(UpdateUserRequest.class))).thenReturn(Future.succeededFuture(null));

    var req = UpdateUserRequest.builder()
        .userId(99).firstName("Ghost").build();

    service.updateUser(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("User not found");
          ctx.completeNow();
        })));
  }

  /* ─── trashUser ─── */

  @Test
  @DisplayName("trashUser soft-deletes user and evicts cache")
  void trashUserSuccess(VertxTestContext ctx) {
    mockTracing();
    var user = aUser();

    when(repo.trashed(1)).thenReturn(Future.succeededFuture(user));
    stubDelete();

    service.trashUser(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getUserId()).isEqualTo(1);
          assertThat(result.getFirstname()).isEqualTo("Alice");
          verify(repo).trashed(1);
          verify(redisService).delete("user:1");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashUser fails when user not found")
  void trashUserNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.trashed(99)).thenReturn(Future.succeededFuture(null));

    service.trashUser(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("User not found");
          ctx.completeNow();
        })));
  }

  /* ─── restoreUser ─── */

  @Test
  @DisplayName("restoreUser restores trashed user and evicts cache")
  void restoreUserSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aUser();
    trashed.setDeletedAt(Timestamp.from(Instant.parse("2026-06-25T10:00:00Z")));
    var restored = aUser();

    when(repoQuery.findByTrashedId(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.restore(1)).thenReturn(Future.succeededFuture(restored));
    stubDelete();

    service.restoreUser(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getUserId()).isEqualTo(1);
          verify(repoQuery).findByTrashedId(1);
          verify(repo).restore(1);
          verify(redisService).delete("user:1");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreUser fails when user is not trashed")
  void restoreUserNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(repoQuery.findByTrashedId(99)).thenReturn(Future.succeededFuture(null));

    service.restoreUser(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("User not found or must be trashed first");
          ctx.completeNow();
        })));
  }

  /* ─── deletePermanent ─── */

  @Test
  @DisplayName("deletePermanent deletes trashed user and evicts cache")
  void deletePermanentSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aUser();
    trashed.setDeletedAt(Timestamp.from(Instant.parse("2026-06-25T10:00:00Z")));

    when(repoQuery.findByTrashedId(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.deletePermanent(1)).thenReturn(Future.succeededFuture(true));
    stubDelete();

    service.deletePermanent(1)
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repoQuery).findByTrashedId(1);
          verify(repo).deletePermanent(1);
          verify(redisService).delete("user:1");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deletePermanent fails when user is not trashed")
  void deletePermanentNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(repoQuery.findByTrashedId(99)).thenReturn(Future.succeededFuture(null));

    service.deletePermanent(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("User not found or must be trashed before permanent deletion");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deletePermanent fails when delete returns false")
  void deletePermanentReturnsFalse(VertxTestContext ctx) {
    mockTracing();
    var trashed = aUser();
    trashed.setDeletedAt(Timestamp.from(Instant.parse("2026-06-25T10:00:00Z")));

    when(repoQuery.findByTrashedId(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.deletePermanent(1)).thenReturn(Future.succeededFuture(false));

    service.deletePermanent(1)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("User not found or must be trashed before permanent deletion");
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllUsers ─── */

  @Test
  @DisplayName("restoreAllUsers restores all and evicts list cache")
  void restoreAllUsersSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAllUsers()).thenReturn(Future.succeededFuture(3));
    when(redisService.delete("user:list:*")).thenReturn(Future.succeededFuture(1L));

    service.restoreAllUsers()
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repo).restoreAllUsers();
          verify(redisService).delete("user:list:*");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreAllUsers fails when no trashed users found")
  void restoreAllUsersNone(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAllUsers()).thenReturn(Future.succeededFuture(0));

    service.restoreAllUsers()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("No trashed users found");
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllPermanentUsers ─── */

  @Test
  @DisplayName("deleteAllPermanentUsers deletes all and evicts list cache")
  void deleteAllPermanentUsersSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanentUsers()).thenReturn(Future.succeededFuture(2));
    when(redisService.delete("user:list:*")).thenReturn(Future.succeededFuture(1L));

    service.deleteAllPermanentUsers()
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repo).deleteAllPermanentUsers();
          verify(redisService).delete("user:list:*");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteAllPermanentUsers fails when no trashed users found")
  void deleteAllPermanentUsersNone(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanentUsers()).thenReturn(Future.succeededFuture(0));

    service.deleteAllPermanentUsers()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("No trashed users found");
          ctx.completeNow();
        })));
  }
}
