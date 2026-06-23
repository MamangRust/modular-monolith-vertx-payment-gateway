package io.example.user.service;

import io.example.user.domain.requests.CreateUserRequest;
import io.example.user.domain.requests.UpdateUserRequest;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.vertx.core.Future;

public interface UserCommandService {
  Future<UserResponse> createUser(CreateUserRequest req);

  Future<UserResponse> updateUser(UpdateUserRequest req);

  Future<UserResponseDeleteAt> trashUser(Integer userId);

  Future<UserResponseDeleteAt> restoreUser(Integer userId);

  Future<Void> deletePermanent(Integer userId);

  Future<Void> restoreAllUsers();

  Future<Void> deleteAllPermanentUsers();
}
