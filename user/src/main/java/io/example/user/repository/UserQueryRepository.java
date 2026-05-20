package io.example.user.repository;

import io.example.common.domain.PagedResult;
import io.example.user.model.User;
import io.vertx.core.Future;

public interface UserQueryRepository {
  Future<PagedResult<User>> getUsers(String search, int page, int pageSize);
  Future<PagedResult<User>> getActiveUsers(String search, int page, int pageSize);
  Future<PagedResult<User>> getTrashedUsers(String search, int page, int pageSize);
  Future<User> getUserById(Integer userId);
  Future<User> getUserByEmail(String email);
}
