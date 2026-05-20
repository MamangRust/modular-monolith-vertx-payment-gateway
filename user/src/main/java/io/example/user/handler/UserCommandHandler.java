package io.example.user.handler;

import com.google.protobuf.Empty;

import io.example.user.service.UserCommandService;
import io.vertx.core.Future;
import pb.user.User.ApiResponseUser;
import pb.user.User.ApiResponseUserDeleteAt;
import pb.user.User.FindByIdUserRequest;
import pb.user.UserCommand.ApiResponseUserAll;
import pb.user.UserCommand.ApiResponseUserDelete;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;

public class UserCommandHandler implements pb.user.VertxUserCommandServiceGrpcServer.UserCommandServiceApi {
  private final UserCommandService service;

  public UserCommandHandler(UserCommandService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseUser> create(CreateUserRequest req) {
    return service.createUser(req)
        .map(res -> {
          ApiResponseUser.Builder builder = ApiResponseUser.newBuilder()
              .setStatus(res.status() != null ? res.status() : "error")
              .setMessage(res.message() != null ? res.message() : "");

          if (res.data() != null) {
            builder.setData(ProtoConverter.toUserResponse(res.data()));
          }

          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseUser> update(UpdateUserRequest req) {
    return service.updateUser(req)
        .map(res -> {
          ApiResponseUser.Builder builder = ApiResponseUser.newBuilder()
              .setStatus(res.status() != null ? res.status() : "error")
              .setMessage(res.message() != null ? res.message() : "");

          if (res.data() != null) {
            builder.setData(ProtoConverter.toUserResponse(res.data()));
          }

          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseUserDeleteAt> trashedUser(FindByIdUserRequest req) {
    return service.trashUser(req)
        .map(res -> {
          ApiResponseUserDeleteAt.Builder builder = ApiResponseUserDeleteAt.newBuilder()
              .setStatus(res.status() != null ? res.status() : "error")
              .setMessage(res.message() != null ? res.message() : "");

          if (res.data() != null) {
            builder.setData(ProtoConverter.toUserDeleteAt(res.data()));
          }

          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseUserDeleteAt> restoreUser(FindByIdUserRequest req) {
    return service.restoreUser(req)
        .map(res -> {
          ApiResponseUserDeleteAt.Builder builder = ApiResponseUserDeleteAt.newBuilder()
              .setStatus(res.status() != null ? res.status() : "error")
              .setMessage(res.message() != null ? res.message() : "");

          if (res.data() != null) {
            builder.setData(ProtoConverter.toUserDeleteAt(res.data()));
          }

          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseUserDelete> deleteUserPermanent(FindByIdUserRequest req) {
    return service.deletePermanent(req)
        .map(res -> ApiResponseUserDelete.newBuilder()
            .setStatus(res.status() != null ? res.status() : "error")
            .setMessage(res.message() != null ? res.message() : "")
            .build());
  }

  @Override
  public Future<ApiResponseUserAll> restoreAllUser(Empty req) {
    return service.restoreAllUsers()
        .map(res -> ApiResponseUserAll.newBuilder()
            .setStatus(res.status() != null ? res.status() : "error")
            .setMessage(res.message() != null ? res.message() : "")
            .build());
  }

  @Override
  public Future<ApiResponseUserAll> deleteAllUserPermanent(Empty req) {
    return service.deleteAllPermanentUsers()
        .map(res -> ApiResponseUserAll.newBuilder()
            .setStatus(res.status() != null ? res.status() : "error")
            .setMessage(res.message() != null ? res.message() : "")
            .build());
  }
}
