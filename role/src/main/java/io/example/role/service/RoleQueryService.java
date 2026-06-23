package io.example.role.service;

import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.role.domain.requests.FindAllRoles;
import io.example.role.model.RoleResponse;
import io.example.role.model.RoleResponseDeleteAt;
import io.vertx.core.Future;

public interface RoleQueryService {
    Future<PagedResult<RoleResponse>> getAllRoles(FindAllRoles req);

    Future<PagedResult<RoleResponseDeleteAt>> getActiveRoles(FindAllRoles req);

    Future<PagedResult<RoleResponseDeleteAt>> getTrashedRoles(FindAllRoles req);

    Future<RoleResponse> getRoleById(Integer roleId);

    Future<List<RoleResponse>> getRolesByUserId(Integer userId);
}