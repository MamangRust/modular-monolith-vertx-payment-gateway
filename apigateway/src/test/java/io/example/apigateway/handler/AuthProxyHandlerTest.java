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

import pb.Auth;
import pb.VertxAuthServiceGrpcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthProxyHandlerTest {

  @Mock private VertxAuthServiceGrpcClient client;
  @Mock private RoutingContext ctx;
  @Mock private RequestBody reqBody;
  @Mock private io.vertx.ext.auth.User user;
  @Mock private io.vertx.core.json.JsonObject principal;

  private AuthProxyHandler handler;
  private MockedStatic<GrpcGatewayUtils> utils;

  @BeforeEach
  void setUp() {
    handler = new AuthProxyHandler(client);
    utils = mockStatic(GrpcGatewayUtils.class);
  }

  @AfterEach
  void tearDown() {
    utils.close();
  }

  @Test
  void register() {
    var body = new JsonObject().put("email", "a@b.com").put("password", "pass");
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(any(), anyString(), anyString())).thenReturn("val");

    when(client.registerUser(any(Auth.RegisterRequest.class)))
        .thenReturn(Future.succeededFuture(Auth.ApiResponseRegister.getDefaultInstance()));

    handler.register(ctx);

    verify(client).registerUser(any(Auth.RegisterRequest.class));
  }

  @Test
  void verifyCode() {
    var body = new JsonObject().put("code", "abc123");
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(any(), anyString(), anyString())).thenReturn("abc123");
    when(client.verifyCode(any(Auth.VerifyCodeRequest.class)))
        .thenReturn(Future.succeededFuture(Auth.ApiResponseVerifyCode.getDefaultInstance()));
    handler.verifyCode(ctx);
    verify(client).verifyCode(any(Auth.VerifyCodeRequest.class));
  }

  @Test
  void login() {
    var body = new JsonObject().put("email", "a@b.com").put("password", "pass");
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(any(), anyString(), anyString())).thenReturn("val");
    when(client.loginUser(any(Auth.LoginRequest.class)))
        .thenReturn(Future.succeededFuture(Auth.ApiResponseLogin.getDefaultInstance()));
    handler.login(ctx);
    verify(client).loginUser(any(Auth.LoginRequest.class));
  }

  @Test
  void refreshToken() {
    var body = new JsonObject().put("refresh_token", "tok");
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(any(), anyString(), anyString())).thenReturn("tok");
    when(client.refreshToken(any(Auth.RefreshTokenRequest.class)))
        .thenReturn(Future.succeededFuture(Auth.ApiResponseRefreshToken.getDefaultInstance()));
    handler.refreshToken(ctx);
    verify(client).refreshToken(any(Auth.RefreshTokenRequest.class));
  }

  @Test
  void getMe() {
    when(ctx.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);
    when(principal.getInteger("userId")).thenReturn(1);

    when(client.getMe(any(Auth.GetMeRequest.class)))
        .thenReturn(Future.succeededFuture(Auth.ApiResponseGetMe.getDefaultInstance()));
    handler.getMe(ctx);
    verify(client).getMe(any(Auth.GetMeRequest.class));
  }

  @Test
  void getMe_noUser() {
    when(ctx.user()).thenReturn(null);
    handler.getMe(ctx);
    utils.verify(() -> GrpcGatewayUtils.sendError(eq(ctx), eq(401), anyString()));
  }

  @Test
  void logout() {
    handler.logout(ctx);
    utils.verify(() -> GrpcGatewayUtils.sendSuccess(eq(ctx), eq(200), anyString()));
  }
}
