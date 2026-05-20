package io.example.role.service;

import java.util.List;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.role.model.RoleResponse;
import io.example.role.model.RoleResponseDeleteAt;
import io.vertx.core.Future;
import pb.role.Role.FindAllRoleRequest;

public interface RoleQueryService {
    Future<ApiResponsePagination<List<RoleResponse>>> getAllRoles(FindAllRoleRequest req);
    Future<ApiResponsePagination<List<RoleResponseDeleteAt>>> getActiveRoles(FindAllRoleRequest req);
    Future<ApiResponsePagination<List<RoleResponseDeleteAt>>> getTrashedRoles(FindAllRoleRequest req);
    Future<ApiResponse<RoleResponse>> getRoleById(Integer roleId);
    Future<ApiResponse<List<RoleResponse>>> getRolesByUserId(Integer userId);
}
