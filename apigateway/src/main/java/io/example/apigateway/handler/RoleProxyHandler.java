package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import pb.role.Role;
import pb.role.RoleCommand;
import pb.role.VertxRoleCommandServiceGrpcClient;
import pb.role.VertxRoleServiceGrpcClient;

@RequiredArgsConstructor
public class RoleProxyHandler {
  private final VertxRoleServiceGrpcClient queryClient;
  private final VertxRoleCommandServiceGrpcClient commandClient;

  private Role.FindAllRoleRequest buildFindAllRoleReq(RoutingContext ctx) {
    return Role.FindAllRoleRequest.newBuilder()
        .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
        .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
        .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
        .build();
  }

  public void findAll(RoutingContext ctx) {
    queryClient.findAllRole(buildFindAllRoleReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void findActive(RoutingContext ctx) {
    queryClient.findByActive(buildFindAllRoleReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void findTrashed(RoutingContext ctx) {
    queryClient.findByTrashed(buildFindAllRoleReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void findById(RoutingContext ctx) {
    var req = Role.FindByIdRoleRequest.newBuilder()
        .setRoleId(GrpcGatewayUtils.getSafePathInt(ctx, "id"))
        .build();
    queryClient.findByIdRole(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void create(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = RoleCommand.CreateRoleRequest.newBuilder()
        .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
        .build();
    commandClient.createRole(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 201))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void update(RoutingContext ctx) {
    int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
    JsonObject body = ctx.body().asJsonObject();
    var req = RoleCommand.UpdateRoleRequest.newBuilder()
        .setId(id)
        .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
        .build();
    commandClient.updateRole(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restore(RoutingContext ctx) {
    var req = Role.FindByIdRoleRequest.newBuilder()
        .setRoleId(GrpcGatewayUtils.getSafePathInt(ctx, "id"))
        .build();
    commandClient.restoreRole(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void trashed(RoutingContext ctx) {
    var req = Role.FindByIdRoleRequest.newBuilder()
        .setRoleId(GrpcGatewayUtils.getSafePathInt(ctx, "id"))
        .build();
    commandClient.trashedRole(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deletePermanent(RoutingContext ctx) {
    var req = Role.FindByIdRoleRequest.newBuilder()
        .setRoleId(GrpcGatewayUtils.getSafePathInt(ctx, "id"))
        .build();
    commandClient.deleteRolePermanent(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreAllRoles(RoutingContext ctx) {
    commandClient.restoreAllRole(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteAllPermanentRoles(RoutingContext ctx) {
    commandClient.deleteAllRolePermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }
}