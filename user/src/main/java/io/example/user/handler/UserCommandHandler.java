package io.example.user.handler;

import com.google.protobuf.Empty;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.user.service.UserCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.user.User.ApiResponseUser;
import pb.user.User.ApiResponseUserDeleteAt;
import pb.user.User.FindByIdUserRequest;
import pb.user.UserCommand.ApiResponseUserAll;
import pb.user.UserCommand.ApiResponseUserDelete;

@RequiredArgsConstructor
public class UserCommandHandler implements pb.user.VertxUserCommandServiceGrpcServer.UserCommandServiceApi {
  private final UserCommandService service;

  @Override
  public Future<ApiResponseUser> create(pb.user.UserCommand.CreateUserRequest req) {
    io.example.user.domain.requests.CreateUserRequest domainReq = io.example.user.domain.requests.CreateUserRequest.builder()
        .firstName(req.getFirstname())
        .lastName(req.getLastname())
        .email(req.getEmail())
        .password(req.getPassword())
        .confirmPassword(req.getConfirmPassword())
        .build();

    return service.createUser(domainReq)
        .map(data -> ApiResponseUser.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.toUserResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseUser> update(pb.user.UserCommand.UpdateUserRequest req) {
    io.example.user.domain.requests.UpdateUserRequest domainReq = io.example.user.domain.requests.UpdateUserRequest.builder()
        .userId(req.getId())
        .firstName(req.getFirstname())
        .lastName(req.getLastname())
        .email(req.getEmail())
        .password(req.getPassword())
        .confirmPassword(req.getConfirmPassword())
        .build();

    return service.updateUser(domainReq)
        .map(data -> ApiResponseUser.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.toUserResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseUserDeleteAt> trashedUser(FindByIdUserRequest req) {
    return service.trashUser(req.getId())
        .map(data -> ApiResponseUserDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.toUserDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseUserDeleteAt> restoreUser(FindByIdUserRequest req) {
    return service.restoreUser(req.getId())
        .map(data -> ApiResponseUserDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.toUserDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseUserDelete> deleteUserPermanent(FindByIdUserRequest req) {
    return service.deletePermanent(req.getId())
        .map(v -> ApiResponseUserDelete.newBuilder()
            .setStatus("success")
            .setMessage("User deleted permanently")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseUserAll> restoreAllUser(Empty req) {
    return service.restoreAllUsers()
        .map(v -> ApiResponseUserAll.newBuilder()
            .setStatus("success")
            .setMessage("All users restored successfully")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseUserAll> deleteAllUserPermanent(Empty req) {
    return service.deleteAllPermanentUsers()
        .map(v -> ApiResponseUserAll.newBuilder()
            .setStatus("success")
            .setMessage("All users permanently deleted")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}