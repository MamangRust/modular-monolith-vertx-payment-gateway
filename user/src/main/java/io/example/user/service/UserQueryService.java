package io.example.user.service;

import java.util.List;

import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.vertx.core.Future;
import pb.user.User.FindAllUserRequest;
import pb.user.User.FindByIdUserRequest;

public interface UserQueryService {
  Future<ApiResponsePagination<List<UserResponse>>> getUsers(FindAllUserRequest req);

  Future<ApiResponsePagination<List<UserResponseDeleteAt>>> getActiveUsers(FindAllUserRequest req);

  Future<ApiResponsePagination<List<UserResponseDeleteAt>>> getTrashedUsers(FindAllUserRequest req);

  Future<ApiResponse<UserResponse>> getUserById(FindByIdUserRequest req);
}
