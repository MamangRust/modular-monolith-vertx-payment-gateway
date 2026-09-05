package io.example.user.handler;

import com.google.protobuf.Empty;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.example.user.service.UserCommandService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.user.User.FindByIdUserRequest;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class UserCommandHandlerTest {

  @Mock
  private UserCommandService service;

  private UserCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new UserCommandHandler(service);
  }

  private static UserResponse aUserResponse() {
    return UserResponse.builder()
        .userId(1)
        .firstname("John")
        .lastname("Doe")
        .email("john@example.com")
        .createdAt("2026-06-26T10:00:00Z")
        .updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static UserResponseDeleteAt aUserResponseDeleteAt() {
    return UserResponseDeleteAt.builder()
        .userId(1)
        .firstname("John")
        .lastname("Doe")
        .email("john@example.com")
        .createdAt("2026-06-26T10:00:00Z")
        .updatedAt("2026-06-26T10:00:00Z")
        .deletedAt("2026-06-25T10:00:00Z")
        .build();
  }

  /* ─── create ─── */

  @Test
  @DisplayName("create delegates and returns response")
  void create(VertxTestContext ctx) {
    when(service.createUser(any())).thenReturn(Future.succeededFuture(aUserResponse()));

    var req = CreateUserRequest.newBuilder()
        .setFirstname("John")
        .setLastname("Doe")
        .setEmail("john@example.com")
        .setPassword("plain_pwd")
        .setConfirmPassword("plain_pwd")
        .build();

    handler.create(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().getFirstname()).isEqualTo("John");
          assertThat(resp.getData().getLastname()).isEqualTo("Doe");
          assertThat(resp.getData().getEmail()).isEqualTo("john@example.com");
          ctx.completeNow();
        })));
  }

  /* ─── update ─── */

  @Test
  @DisplayName("update delegates and returns response")
  void update(VertxTestContext ctx) {
    when(service.updateUser(any())).thenReturn(Future.succeededFuture(aUserResponse()));

    var req = UpdateUserRequest.newBuilder()
        .setId(1)
        .setFirstname("John")
        .setLastname("Doe")
        .setEmail("john@example.com")
        .build();

    handler.update(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── trashedUser ─── */

  @Test
  @DisplayName("trashedUser delegates and returns delete-at response")
  void trashedUser(VertxTestContext ctx) {
    when(service.trashUser(1)).thenReturn(Future.succeededFuture(aUserResponseDeleteAt()));

    var req = FindByIdUserRequest.newBuilder().setId(1).build();

    handler.trashedUser(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().hasDeletedAt()).isTrue();
          ctx.completeNow();
        })));
  }

  /* ─── restoreUser ─── */

  @Test
  @DisplayName("restoreUser delegates and returns delete-at response")
  void restoreUser(VertxTestContext ctx) {
    when(service.restoreUser(1)).thenReturn(Future.succeededFuture(aUserResponseDeleteAt()));

    var req = FindByIdUserRequest.newBuilder().setId(1).build();

    handler.restoreUser(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── deleteUserPermanent ─── */

  @Test
  @DisplayName("deleteUserPermanent delegates and returns success")
  void deleteUserPermanent(VertxTestContext ctx) {
    when(service.deletePermanent(1)).thenReturn(Future.succeededFuture());

    var req = FindByIdUserRequest.newBuilder().setId(1).build();

    handler.deleteUserPermanent(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("User deleted permanently");
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllUser ─── */

  @Test
  @DisplayName("restoreAllUser delegates and returns success")
  void restoreAllUser(VertxTestContext ctx) {
    when(service.restoreAllUsers()).thenReturn(Future.succeededFuture());

    handler.restoreAllUser(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("All users restored successfully");
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllUserPermanent ─── */

  @Test
  @DisplayName("deleteAllUserPermanent delegates and returns success")
  void deleteAllUserPermanent(VertxTestContext ctx) {
    when(service.deleteAllPermanentUsers()).thenReturn(Future.succeededFuture());

    handler.deleteAllUserPermanent(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("All users permanently deleted");
          ctx.completeNow();
        })));
  }

  /* ─── error path ─── */

  @Test
  @DisplayName("create delegates error when service fails")
  void createError(VertxTestContext ctx) {
    when(service.createUser(any()))
        .thenReturn(Future.failedFuture(new RuntimeException("DB error")));

    var req = CreateUserRequest.newBuilder().setFirstname("John").build();

    handler.create(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }
}
