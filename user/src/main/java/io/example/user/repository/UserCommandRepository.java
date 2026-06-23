package io.example.user.repository;

import io.example.user.domain.requests.CreateUserRequest;
import io.example.user.domain.requests.UpdatePasswordRequest;
import io.example.user.domain.requests.UpdateUserRequest;
import io.example.user.model.User;
import io.vertx.core.Future;

public interface UserCommandRepository {
  Future<User> createUser(CreateUserRequest request);

  Future<Void> assignDefaultAdminRole(Integer userId);

  Future<User> updateUser(UpdateUserRequest request);

  Future<User> updatePassword(UpdatePasswordRequest request);

  Future<User> restore(Integer userId);

  Future<User> trashed(Integer userId);

  Future<Boolean> deletePermanent(Integer userId);

  Future<Integer> restoreAllUsers();

  Future<Integer> deleteAllPermanentUsers();
}
