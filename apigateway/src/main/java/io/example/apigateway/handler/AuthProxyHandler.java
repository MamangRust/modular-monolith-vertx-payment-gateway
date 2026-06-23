package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import pb.Auth;
import pb.VertxAuthServiceGrpcClient;

@RequiredArgsConstructor
public class AuthProxyHandler {
  private final VertxAuthServiceGrpcClient client;

  public void register(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = Auth.RegisterRequest.newBuilder()
        .setFirstname(GrpcGatewayUtils.getJsonString(body, "firstname", ""))
        .setLastname(GrpcGatewayUtils.getJsonString(body, "lastname", ""))
        .setEmail(GrpcGatewayUtils.getJsonString(body, "email", ""))
        .setPassword(GrpcGatewayUtils.getJsonString(body, "password", ""))
        .build();

    client.registerUser(req)
        .onSuccess(resp -> GrpcGatewayUtils.sendResponse(ctx, resp, 201))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void login(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = Auth.LoginRequest.newBuilder()
        .setEmail(GrpcGatewayUtils.getJsonString(body, "email", ""))
        .setPassword(GrpcGatewayUtils.getJsonString(body, "password", ""))
        .build();

    client.loginUser(req)
        .onSuccess(resp -> GrpcGatewayUtils.sendResponse(ctx, resp, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void refreshToken(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = Auth.RefreshTokenRequest.newBuilder()
        .setRefreshToken(GrpcGatewayUtils.getJsonString(body, "refresh_token", ""))
        .build();

    client.refreshToken(req)
        .onSuccess(resp -> GrpcGatewayUtils.sendResponse(ctx, resp, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMe(RoutingContext ctx) {
    if (ctx.user() == null || ctx.user().principal() == null) {
      GrpcGatewayUtils.sendError(ctx, 401, "No valid authentication provided");
      return;
    }

    Integer userId = ctx.user().principal().getInteger("userId");
    if (userId == null || userId == 0) {
      GrpcGatewayUtils.sendError(ctx, 401, "Invalid token payload");
      return;
    }

    var req = Auth.GetMeRequest.newBuilder().setUserId(userId).build();
    client.getMe(req)
        .onSuccess(resp -> GrpcGatewayUtils.sendResponse(ctx, resp, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void logout(RoutingContext ctx) {
    GrpcGatewayUtils.sendSuccess(ctx, 200, "Successfully logged out");
  }
}