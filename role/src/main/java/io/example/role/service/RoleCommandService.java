package io.example.role.service;

import io.example.common.model.ApiResponse;
import io.example.role.model.RoleResponse;
import io.example.role.model.RoleResponseDeleteAt;
import io.vertx.core.Future;
import pb.role.RoleCommand.CreateRoleRequest;
import pb.role.RoleCommand.UpdateRoleRequest;

public interface RoleCommandService {
    Future<ApiResponse<RoleResponse>> createRole(CreateRoleRequest req);
    Future<ApiResponse<RoleResponse>> updateRole(UpdateRoleRequest req);
    Future<ApiResponse<RoleResponseDeleteAt>> trashRole(Integer roleId);
    Future<ApiResponse<RoleResponseDeleteAt>> restoreRole(Integer roleId);
    Future<ApiResponse<Void>> deletePermanent(Integer roleId);
    Future<ApiResponse<Void>> restoreAllRoles();
    Future<ApiResponse<Void>> deleteAllPermanentRoles();
}
