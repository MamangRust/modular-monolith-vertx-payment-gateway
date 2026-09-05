package io.example.role.handler;

import io.example.role.domain.requests.FindAllRoles;
import io.example.role.model.RoleResponse;
import io.example.role.service.RoleQueryService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.role.Role.FindByIdRoleRequest;
import pb.role.Role.FindAllRoleRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class RoleQueryHandlerTest {
  @Mock private RoleQueryService service;
  private RoleQueryHandler handler;
  @BeforeEach void setUp() { handler = new RoleQueryHandler(service); }

  @Test void findByIdRole(VertxTestContext ctx) {
    when(service.getRoleById(1)).thenReturn(Future.succeededFuture(RoleResponse.builder().id(1).name("ROLE_ADMIN").build()));
    handler.findByIdRole(FindByIdRoleRequest.newBuilder().setRoleId(1).build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow(); })));
  }

  @Test void findByIdRole_error(VertxTestContext ctx) {
    when(service.getRoleById(99)).thenReturn(Future.failedFuture(new RuntimeException("Not found")));
    handler.findByIdRole(FindByIdRoleRequest.newBuilder().setRoleId(99).build())
        .onComplete(ctx.failing(e -> ctx.verify(() -> { assertThat(e).isInstanceOf(io.grpc.StatusRuntimeException.class); ctx.completeNow(); })));
  }
}
