package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.role.Role;
import pb.role.RoleCommand;
import pb.role.VertxRoleCommandServiceGrpcClient;
import pb.role.VertxRoleServiceGrpcClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleProxyHandlerTest {

  @Mock private VertxRoleServiceGrpcClient queryClient;
  @Mock private VertxRoleCommandServiceGrpcClient commandClient;
  @Mock private RoutingContext ctx;
  @Mock private RequestBody reqBody;

  private RoleProxyHandler handler;
  private MockedStatic<GrpcGatewayUtils> utils;

  @BeforeEach
  void setUp() {
    handler = new RoleProxyHandler(queryClient, commandClient);
    utils = mockStatic(GrpcGatewayUtils.class);
  }

  @AfterEach
  void tearDown() {
    utils.close();
  }

  @Test
  void findAll() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);

    when(queryClient.findAllRole(any(Role.FindAllRoleRequest.class)))
        .thenReturn(Future.succeededFuture(pb.role.RoleQuery.ApiResponsePaginationRole.getDefaultInstance()));

    handler.findAll(ctx);
    verify(queryClient).findAllRole(any(Role.FindAllRoleRequest.class));
  }

  @Test
  void findActive() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);

    when(queryClient.findByActive(any(Role.FindAllRoleRequest.class)))
        .thenReturn(Future.succeededFuture(pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt.getDefaultInstance()));

    handler.findActive(ctx);
    verify(queryClient).findByActive(any(Role.FindAllRoleRequest.class));
  }

  @Test
  void findTrashed() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);

    when(queryClient.findByTrashed(any(Role.FindAllRoleRequest.class)))
        .thenReturn(Future.succeededFuture(pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt.getDefaultInstance()));

    handler.findTrashed(ctx);
    verify(queryClient).findByTrashed(any(Role.FindAllRoleRequest.class));
  }

  @Test
  void findById() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "id")).thenReturn(5);
    when(queryClient.findByIdRole(any(Role.FindByIdRoleRequest.class)))
        .thenReturn(Future.succeededFuture(Role.ApiResponseRole.getDefaultInstance()));

    handler.findById(ctx);
    verify(queryClient).findByIdRole(any(Role.FindByIdRoleRequest.class));
  }

  @Test
  void create() {
    var body = new JsonObject().put("name", "ADMIN");
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("name"), anyString())).thenReturn("ADMIN");

    when(commandClient.createRole(any(RoleCommand.CreateRoleRequest.class)))
        .thenReturn(Future.succeededFuture(Role.ApiResponseRole.getDefaultInstance()));

    handler.create(ctx);
    verify(commandClient).createRole(any(RoleCommand.CreateRoleRequest.class));
  }

  @Test
  void update() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "id")).thenReturn(5);
    var body = new JsonObject().put("name", "ADMIN");
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("name"), anyString())).thenReturn("ADMIN");

    when(commandClient.updateRole(any(RoleCommand.UpdateRoleRequest.class)))
        .thenReturn(Future.succeededFuture(Role.ApiResponseRole.getDefaultInstance()));

    handler.update(ctx);
    verify(commandClient).updateRole(any(RoleCommand.UpdateRoleRequest.class));
  }

  @Test
  void restore() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "id")).thenReturn(5);
    when(commandClient.restoreRole(any(Role.FindByIdRoleRequest.class)))
        .thenReturn(Future.succeededFuture(Role.ApiResponseRoleDeleteAt.getDefaultInstance()));

    handler.restore(ctx);
    verify(commandClient).restoreRole(any(Role.FindByIdRoleRequest.class));
  }

  @Test
  void trashed() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "id")).thenReturn(5);
    when(commandClient.trashedRole(any(Role.FindByIdRoleRequest.class)))
        .thenReturn(Future.succeededFuture(Role.ApiResponseRoleDeleteAt.getDefaultInstance()));

    handler.trashed(ctx);
    verify(commandClient).trashedRole(any(Role.FindByIdRoleRequest.class));
  }

  @Test
  void deletePermanent() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "id")).thenReturn(5);
    when(commandClient.deleteRolePermanent(any(Role.FindByIdRoleRequest.class)))
        .thenReturn(Future.succeededFuture(RoleCommand.ApiResponseRoleDelete.getDefaultInstance()));

    handler.deletePermanent(ctx);
    verify(commandClient).deleteRolePermanent(any(Role.FindByIdRoleRequest.class));
  }

  @Test
  void restoreAllRoles() {
    when(commandClient.restoreAllRole(any(com.google.protobuf.Empty.class)))
        .thenReturn(Future.succeededFuture(RoleCommand.ApiResponseRoleAll.getDefaultInstance()));

    handler.restoreAllRoles(ctx);
    verify(commandClient).restoreAllRole(any(com.google.protobuf.Empty.class));
  }

  @Test
  void deleteAllPermanentRoles() {
    when(commandClient.deleteAllRolePermanent(any(com.google.protobuf.Empty.class)))
        .thenReturn(Future.succeededFuture(RoleCommand.ApiResponseRoleAll.getDefaultInstance()));

    handler.deleteAllPermanentRoles(ctx);
    verify(commandClient).deleteAllRolePermanent(any(com.google.protobuf.Empty.class));
  }
}
