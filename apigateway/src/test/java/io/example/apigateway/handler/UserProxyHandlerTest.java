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
import pb.user.User;
import pb.user.UserCommand;
import pb.user.UserQuery;
import pb.user.VertxUserCommandServiceGrpcClient;
import pb.user.VertxUserQueryServiceGrpcClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProxyHandlerTest {

  @Mock private VertxUserQueryServiceGrpcClient queryClient;
  @Mock private VertxUserCommandServiceGrpcClient commandClient;
  @Mock private RoutingContext ctx;
  @Mock private RequestBody reqBody;

  private UserProxyHandler handler;
  private MockedStatic<GrpcGatewayUtils> utils;

  @BeforeEach
  void setUp() {
    handler = new UserProxyHandler(queryClient, commandClient);
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

    when(queryClient.findAll(any(User.FindAllUserRequest.class)))
        .thenReturn(Future.succeededFuture(UserQuery.ApiResponsePaginationUser.getDefaultInstance()));

    handler.findAll(ctx);
    verify(queryClient).findAll(any(User.FindAllUserRequest.class));
  }

  @Test
  void findActive() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);

    when(queryClient.findByActive(any(User.FindAllUserRequest.class)))
        .thenReturn(Future.succeededFuture(UserQuery.ApiResponsePaginationUserDeleteAt.getDefaultInstance()));

    handler.findActive(ctx);
    verify(queryClient).findByActive(any(User.FindAllUserRequest.class));
  }

  @Test
  void findTrashed() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);

    when(queryClient.findByTrashed(any(User.FindAllUserRequest.class)))
        .thenReturn(Future.succeededFuture(UserQuery.ApiResponsePaginationUserDeleteAt.getDefaultInstance()));

    handler.findTrashed(ctx);
    verify(queryClient).findByTrashed(any(User.FindAllUserRequest.class));
  }

  @Test
  void findById() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "id")).thenReturn(42);

    when(queryClient.findById(any(User.FindByIdUserRequest.class)))
        .thenReturn(Future.succeededFuture(User.ApiResponseUser.getDefaultInstance()));

    handler.findById(ctx);
    verify(queryClient).findById(any(User.FindByIdUserRequest.class));
  }

  @Test
  void update() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "id")).thenReturn(42);
    var body = new JsonObject().put("firstname", "John").put("lastname", "Doe");
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), anyString(), anyString())).thenReturn("val");

    when(commandClient.update(any(UserCommand.UpdateUserRequest.class)))
        .thenReturn(Future.succeededFuture(User.ApiResponseUser.getDefaultInstance()));

    handler.update(ctx);
    verify(commandClient).update(any(UserCommand.UpdateUserRequest.class));
  }

  @Test
  void restore() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "id")).thenReturn(42);

    when(commandClient.restoreUser(any(User.FindByIdUserRequest.class)))
        .thenReturn(Future.succeededFuture(User.ApiResponseUserDeleteAt.getDefaultInstance()));

    handler.restore(ctx);
    verify(commandClient).restoreUser(any(User.FindByIdUserRequest.class));
  }

  @Test
  void trashed() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "id")).thenReturn(42);

    when(commandClient.trashedUser(any(User.FindByIdUserRequest.class)))
        .thenReturn(Future.succeededFuture(User.ApiResponseUserDeleteAt.getDefaultInstance()));

    handler.trashed(ctx);
    verify(commandClient).trashedUser(any(User.FindByIdUserRequest.class));
  }

  @Test
  void deletePermanent() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "id")).thenReturn(42);

    when(commandClient.deleteUserPermanent(any(User.FindByIdUserRequest.class)))
        .thenReturn(Future.succeededFuture(UserCommand.ApiResponseUserDelete.getDefaultInstance()));

    handler.deletePermanent(ctx);
    verify(commandClient).deleteUserPermanent(any(User.FindByIdUserRequest.class));
  }

  @Test
  void restoreAllUsers() {
    when(commandClient.restoreAllUser(any(com.google.protobuf.Empty.class)))
        .thenReturn(Future.succeededFuture(UserCommand.ApiResponseUserAll.getDefaultInstance()));

    handler.restoreAllUsers(ctx);
    verify(commandClient).restoreAllUser(any(com.google.protobuf.Empty.class));
  }

  @Test
  void deleteAllPermanentUsers() {
    when(commandClient.deleteAllUserPermanent(any(com.google.protobuf.Empty.class)))
        .thenReturn(Future.succeededFuture(UserCommand.ApiResponseUserAll.getDefaultInstance()));

    handler.deleteAllPermanentUsers(ctx);
    verify(commandClient).deleteAllUserPermanent(any(com.google.protobuf.Empty.class));
  }
}
