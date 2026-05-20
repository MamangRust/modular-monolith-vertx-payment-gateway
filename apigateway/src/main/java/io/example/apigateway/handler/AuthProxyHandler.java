package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.Auth;
import pb.VertxAuthServiceGrpcClient;

public class AuthProxyHandler {
  private final VertxAuthServiceGrpcClient client;

  public AuthProxyHandler(VertxAuthServiceGrpcClient client) {
    this.client = client;
  }

  public void register(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = Auth.RegisterRequest.newBuilder()
        .setFirstname(body.getString("firstname", ""))
        .setLastname(body.getString("lastname", ""))
        .setEmail(body.getString("email", ""))
        .setPassword(body.getString("password", ""))
        .build();

    client.registerUser(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 201))
        .onFailure(err -> ctx.fail(err));
  }

  public void login(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = Auth.LoginRequest.newBuilder()
        .setEmail(body.getString("email", ""))
        .setPassword(body.getString("password", ""))
        .build();

    client.loginUser(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(err -> ctx.fail(err));
  }

  public void refreshToken(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = Auth.RefreshTokenRequest.newBuilder()
        .setRefreshToken(body.getString("refresh_token", ""))
        .build();

    client.refreshToken(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(err -> ctx.fail(err));
  }

  public void getMe(RoutingContext ctx) {
    if (ctx.user() == null || ctx.user().principal() == null) {
      ctx.response().setStatusCode(401).end("Unauthorized");
      return;
    }
    int userId = ctx.user().principal().getInteger("userId", 0);
    var req = Auth.GetMeRequest.newBuilder()
        .setUserId(userId)
        .build();

    client.getMe(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(err -> ctx.fail(err));
  }

  public void logout(RoutingContext ctx) {
    // Traditional logout does local storage clear. On server side it can clear session if stateful.
    // Proto doesn't strictly define explicit logout rpc, but let's response 200 OK immediately!
    ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(new JsonObject().put("status", 200).put("message", "Successfully logged out").encode());
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
