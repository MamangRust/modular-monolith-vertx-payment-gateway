package io.example.role.service;

import io.example.role.domain.requests.CreateRoleRequest;
import io.example.role.domain.requests.UpdateRoleRequest;
import io.example.role.model.RoleResponse;
import io.example.role.model.RoleResponseDeleteAt;
import io.vertx.core.Future;

public interface RoleCommandService {
    Future<RoleResponse> createRole(CreateRoleRequest req);

    Future<RoleResponse> updateRole(UpdateRoleRequest req);

    Future<RoleResponseDeleteAt> trashRole(Integer roleId);

    Future<RoleResponseDeleteAt> restoreRole(Integer roleId);

    Future<Void> deletePermanent(Integer roleId);

    Future<Void> restoreAllRoles();

    Future<Void> deleteAllPermanentRoles();
}