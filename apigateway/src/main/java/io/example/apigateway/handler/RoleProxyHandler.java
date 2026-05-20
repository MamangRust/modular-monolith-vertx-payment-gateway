package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.role.Role;
import pb.role.RoleCommand;
import pb.role.VertxRoleCommandServiceGrpcClient;
import pb.role.VertxRoleServiceGrpcClient;

public class RoleProxyHandler {
  private final VertxRoleServiceGrpcClient queryClient;
  private final VertxRoleCommandServiceGrpcClient commandClient;

  public RoleProxyHandler(VertxRoleServiceGrpcClient queryClient, VertxRoleCommandServiceGrpcClient commandClient) {
    this.queryClient = queryClient;
    this.commandClient = commandClient;
  }

  public void findAll(RoutingContext ctx) {
    var req = Role.FindAllRoleRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();

    queryClient.findAllRole(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void findActive(RoutingContext ctx) {
    var req = Role.FindAllRoleRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();

    queryClient.findByActive(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void findTrashed(RoutingContext ctx) {
    var req = Role.FindAllRoleRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();

    queryClient.findByTrashed(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void findById(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    var req = Role.FindByIdRoleRequest.newBuilder().setRoleId(id).build();

    queryClient.findByIdRole(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void create(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = RoleCommand.CreateRoleRequest.newBuilder()
        .setName(body.getString("name", ""))
        .build();

    commandClient.createRole(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 201))
        .onFailure(ctx::fail);
  }

  public void update(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();
    var req = RoleCommand.UpdateRoleRequest.newBuilder()
        .setId(id)
        .setName(body.getString("name", ""))
        .build();

    commandClient.updateRole(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void restore(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    var req = Role.FindByIdRoleRequest.newBuilder().setRoleId(id).build();

    commandClient.restoreRole(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void trashed(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    var req = Role.FindByIdRoleRequest.newBuilder().setRoleId(id).build();

    commandClient.trashedRole(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void deletePermanent(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    var req = Role.FindByIdRoleRequest.newBuilder().setRoleId(id).build();

    commandClient.deleteRolePermanent(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void restoreAllRoles(RoutingContext ctx) {
    commandClient.restoreAllRole(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void deleteAllPermanentRoles(RoutingContext ctx) {
    commandClient.deleteAllRolePermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  private void sendResponse(RoutingContext ctx, com.google.protobuf.MessageOrBuilder proto, int defaultStatus) {
    JsonObject json = ProtoMapper.toJson(proto);
    int status = json.getInteger("status", defaultStatus);
    ctx.response()
        .setStatusCode(status == 0 ? defaultStatus : status)
        .putHeader("Content-Type", "application/json")
        .end(json.encode());
  }
}
