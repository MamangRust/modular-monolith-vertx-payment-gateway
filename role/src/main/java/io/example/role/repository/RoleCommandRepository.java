package io.example.role.repository;

import io.example.role.model.Role;
import io.vertx.core.Future;

public interface RoleCommandRepository {
    Future<Role> createRole(String name);
    Future<Role> updateRole(Integer roleId, String name);
    Future<Role> trashed(Integer roleId);
    Future<Role> restore(Integer roleId);
    Future<Void> deletePermanent(Integer roleId);
    Future<Void> restoreAllRoles();
    Future<Void> deleteAllPermanentRoles();
}
