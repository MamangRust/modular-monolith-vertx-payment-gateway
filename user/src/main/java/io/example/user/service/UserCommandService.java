package io.example.user.service;

import io.example.common.model.ApiResponse;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.vertx.core.Future;
import pb.user.User.FindByIdUserRequest;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;

public interface UserCommandService {
  Future<ApiResponse<UserResponse>> createUser(CreateUserRequest req);

  Future<ApiResponse<UserResponse>> updateUser(UpdateUserRequest req);

  Future<ApiResponse<UserResponseDeleteAt>> trashUser(FindByIdUserRequest req);

  Future<ApiResponse<UserResponseDeleteAt>> restoreUser(FindByIdUserRequest req);

  Future<ApiResponse<Void>> deletePermanent(FindByIdUserRequest req);

  Future<ApiResponse<Void>> restoreAllUsers();

  Future<ApiResponse<Void>> deleteAllPermanentUsers();
}
