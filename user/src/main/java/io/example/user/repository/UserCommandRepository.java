package io.example.user.repository;

import io.example.user.model.User;
import io.vertx.core.Future;

public interface UserCommandRepository {
  Future<User> createUser(String firstname, String lastname, String email, String password);
  Future<Void> assignDefaultAdminRole(Integer userId);
  Future<User> updateUser(Integer userId, String firstname, String lastname, String email);
  Future<User> updatePassword(Integer userId, String password);
  Future<User> restore(Integer userId);
  Future<User> trashed(Integer userId);
  Future<Void> deletePermanent(Integer userId);
  Future<Void> restoreAllUsers();
  Future<Void> deleteAllPermanentUsers();
}
