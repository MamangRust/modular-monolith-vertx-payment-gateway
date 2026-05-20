package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.user.User;
import pb.user.UserCommand;
import pb.user.VertxUserCommandServiceGrpcClient;
import pb.user.VertxUserQueryServiceGrpcClient;

public class UserProxyHandler {
  private final VertxUserQueryServiceGrpcClient queryClient;
  private final VertxUserCommandServiceGrpcClient commandClient;

  public UserProxyHandler(VertxUserQueryServiceGrpcClient queryClient, VertxUserCommandServiceGrpcClient commandClient) {
    this.queryClient = queryClient;
    this.commandClient = commandClient;
  }

  public void findAll(RoutingContext ctx) {
    var req = User.FindAllUserRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();

    queryClient.findAll(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void findActive(RoutingContext ctx) {
    var req = User.FindAllUserRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();

    queryClient.findByActive(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void findTrashed(RoutingContext ctx) {
    var req = User.FindAllUserRequest.newBuilder()
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
    var req = User.FindByIdUserRequest.newBuilder().setId(id).build();

    queryClient.findById(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void update(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();
    var req = UserCommand.UpdateUserRequest.newBuilder()
        .setId(id)
        .setFirstname(body.getString("firstname", ""))
        .setLastname(body.getString("lastname", ""))
        .setEmail(body.getString("email", ""))
        .setPassword(body.getString("password", ""))
        .build();

    commandClient.update(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void restore(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    var req = User.FindByIdUserRequest.newBuilder().setId(id).build();

    commandClient.restoreUser(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void trashed(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    var req = User.FindByIdUserRequest.newBuilder().setId(id).build();

    commandClient.trashedUser(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void deletePermanent(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    var req = User.FindByIdUserRequest.newBuilder().setId(id).build();

    commandClient.deleteUserPermanent(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void restoreAllUsers(RoutingContext ctx) {
    commandClient.restoreAllUser(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void deleteAllPermanentUsers(RoutingContext ctx) {
    commandClient.deleteAllUserPermanent(com.google.protobuf.Empty.getDefaultInstance())
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
