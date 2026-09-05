package io.example.role.handler;

import com.google.protobuf.Empty;
import io.example.role.model.RoleResponse;
import io.example.role.model.RoleResponseDeleteAt;
import io.example.role.service.RoleCommandService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.role.RoleCommand.ApiResponseRoleAll;
import pb.role.RoleCommand.ApiResponseRoleDelete;
import pb.role.RoleCommand.CreateRoleRequest;
import pb.role.RoleCommand.UpdateRoleRequest;
import pb.role.Role.FindByIdRoleRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class RoleCommandHandlerTest {
  @Mock private RoleCommandService service;
  private RoleCommandHandler handler;
  @BeforeEach void setUp() { handler = new RoleCommandHandler(service); }

  @Test void createRole(VertxTestContext ctx) {
    when(service.createRole(any())).thenReturn(Future.succeededFuture(RoleResponse.builder().id(1).name("ROLE_ADMIN").build()));
    handler.createRole(CreateRoleRequest.newBuilder().setName("ROLE_ADMIN").build())
        .onComplete(ctx.succeeding((pb.role.Role.ApiResponseRole r) -> ctx.verify(() -> {
          assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow();
        })));
  }

  @Test void updateRole(VertxTestContext ctx) {
    when(service.updateRole(any())).thenReturn(Future.succeededFuture(RoleResponse.builder().id(1).build()));
    handler.updateRole(UpdateRoleRequest.newBuilder().setId(1).setName("ROLE_USER").build())
        .onComplete(ctx.succeeding((pb.role.Role.ApiResponseRole r) -> ctx.verify(() -> {
          assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow();
        })));
  }

  @Test void trashedRole(VertxTestContext ctx) {
    when(service.trashRole(1)).thenReturn(Future.succeededFuture(RoleResponseDeleteAt.builder().id(1).build()));
    handler.trashedRole(FindByIdRoleRequest.newBuilder().setRoleId(1).build())
        .onComplete(ctx.succeeding((pb.role.Role.ApiResponseRoleDeleteAt r) -> ctx.verify(() -> {
          assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow();
        })));
  }

  @Test void restoreRole(VertxTestContext ctx) {
    when(service.restoreRole(1)).thenReturn(Future.succeededFuture(RoleResponseDeleteAt.builder().id(1).build()));
    handler.restoreRole(FindByIdRoleRequest.newBuilder().setRoleId(1).build())
        .onComplete(ctx.succeeding((pb.role.Role.ApiResponseRoleDeleteAt r) -> ctx.verify(() -> {
          assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow();
        })));
  }

  @Test void deleteRolePermanent(VertxTestContext ctx) {
    when(service.deletePermanent(1)).thenReturn(Future.succeededFuture());
    handler.deleteRolePermanent(FindByIdRoleRequest.newBuilder().setRoleId(1).build())
        .onComplete(ctx.succeeding((ApiResponseRoleDelete r) -> ctx.verify(() -> {
          assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow();
        })));
  }

  @Test void restoreAllRole(VertxTestContext ctx) {
    when(service.restoreAllRoles()).thenReturn(Future.succeededFuture());
    handler.restoreAllRole(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding((ApiResponseRoleAll r) -> ctx.verify(() -> {
          assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow();
        })));
  }

  @Test void deleteAllRolePermanent(VertxTestContext ctx) {
    when(service.deleteAllPermanentRoles()).thenReturn(Future.succeededFuture());
    handler.deleteAllRolePermanent(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding((ApiResponseRoleAll r) -> ctx.verify(() -> {
          assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow();
        })));
  }
}
