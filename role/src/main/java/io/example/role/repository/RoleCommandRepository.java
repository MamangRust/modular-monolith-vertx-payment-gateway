package io.example.role.repository;

import io.example.role.domain.requests.CreateRoleRequest;
import io.example.role.domain.requests.UpdateRoleRequest;
import io.example.role.model.Role;
import io.vertx.core.Future;

public interface RoleCommandRepository {
    Future<Role> createRole(CreateRoleRequest request);

    Future<Role> updateRole(UpdateRoleRequest request);

    Future<Role> trashed(Integer roleId);

    Future<Role> restore(Integer roleId);

    Future<Boolean> deletePermanent(Integer roleId);

    Future<Integer> restoreAllRoles();

    Future<Integer> deleteAllPermanentRoles();
}
