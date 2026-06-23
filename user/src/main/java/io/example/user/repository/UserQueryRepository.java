package io.example.user.repository;

import io.example.common.domain.PagedResult;
import io.example.user.domain.requests.FindAllUsers;
import io.example.user.model.User;
import io.vertx.core.Future;

public interface UserQueryRepository {
  Future<PagedResult<User>> getUsers(FindAllUsers request);

  Future<PagedResult<User>> getActiveUsers(FindAllUsers request);

  Future<PagedResult<User>> getTrashedUsers(FindAllUsers request);

  Future<User> findByTrashedId(Integer userId);

  Future<User> getUserById(Integer userId);

  Future<User> getUserByEmail(String email);
}
