package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import pb.user.User;
import pb.user.UserCommand;
import pb.user.VertxUserCommandServiceGrpcClient;
import pb.user.VertxUserQueryServiceGrpcClient;

@RequiredArgsConstructor
public class UserProxyHandler {
  private final VertxUserQueryServiceGrpcClient queryClient;
  private final VertxUserCommandServiceGrpcClient commandClient;

  private User.FindAllUserRequest buildFindAllUserReq(RoutingContext ctx) {
    return User.FindAllUserRequest.newBuilder()
        .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
        .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
        .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
        .build();
  }

  public void findAll(RoutingContext ctx) {
    queryClient.findAll(buildFindAllUserReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void findActive(RoutingContext ctx) {
    queryClient.findByActive(buildFindAllUserReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void findTrashed(RoutingContext ctx) {
    queryClient.findByTrashed(buildFindAllUserReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void findById(RoutingContext ctx) {
    var req = User.FindByIdUserRequest.newBuilder()
        .setId(GrpcGatewayUtils.getSafePathInt(ctx, "id"))
        .build();
    queryClient.findById(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void update(RoutingContext ctx) {
    int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
    JsonObject body = ctx.body().asJsonObject();
    var req = UserCommand.UpdateUserRequest.newBuilder()
        .setId(id)
        .setFirstname(GrpcGatewayUtils.getJsonString(body, "firstname", ""))
        .setLastname(GrpcGatewayUtils.getJsonString(body, "lastname", ""))
        .setEmail(GrpcGatewayUtils.getJsonString(body, "email", ""))
        .setPassword(GrpcGatewayUtils.getJsonString(body, "password", ""))
        .setConfirmPassword(GrpcGatewayUtils.getJsonString(body, "confirm_password", ""))
        .build();
    commandClient.update(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restore(RoutingContext ctx) {
    var req = User.FindByIdUserRequest.newBuilder()
        .setId(GrpcGatewayUtils.getSafePathInt(ctx, "id"))
        .build();
    commandClient.restoreUser(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void trashed(RoutingContext ctx) {
    var req = User.FindByIdUserRequest.newBuilder()
        .setId(GrpcGatewayUtils.getSafePathInt(ctx, "id"))
        .build();
    commandClient.trashedUser(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deletePermanent(RoutingContext ctx) {
    var req = User.FindByIdUserRequest.newBuilder()
        .setId(GrpcGatewayUtils.getSafePathInt(ctx, "id"))
        .build();
    commandClient.deleteUserPermanent(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreAllUsers(RoutingContext ctx) {
    commandClient.restoreAllUser(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteAllPermanentUsers(RoutingContext ctx) {
    commandClient.deleteAllUserPermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }
}