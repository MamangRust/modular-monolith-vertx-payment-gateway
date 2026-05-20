package io.example.auth.repository;

import io.example.auth.model.UserRole;
import io.vertx.core.Future;

public interface UserRoleRepository {
    Future<UserRole> assignRoleToUser(Integer userId, Integer roleId);
    Future<Void> removeRoleFromUser(Integer userId, Integer roleId);
}
