package io.example.user.service;

import io.example.common.domain.PagedResult;
import io.example.user.domain.requests.FindAllUsers;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.vertx.core.Future;

public interface UserQueryService {
  Future<PagedResult<UserResponse>> getUsers(FindAllUsers req);

  Future<PagedResult<UserResponseDeleteAt>> getActiveUsers(FindAllUsers req);

  Future<PagedResult<UserResponseDeleteAt>> getTrashedUsers(FindAllUsers req);

  Future<UserResponse> getUserById(Integer req);
}

