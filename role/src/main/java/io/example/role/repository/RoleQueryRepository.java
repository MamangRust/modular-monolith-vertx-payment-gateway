package io.example.role.repository;

import java.util.List;
import io.example.common.domain.PagedResult;
import io.example.role.model.Role;
import io.vertx.core.Future;

public interface RoleQueryRepository {
    Future<PagedResult<Role>> getRoles(String search, int page, int pageSize);
    Future<PagedResult<Role>> getActiveRoles(String search, int page, int pageSize);
    Future<PagedResult<Role>> getTrashedRoles(String search, int page, int pageSize);
    Future<Role> getRoleById(Integer roleId);
    Future<Role> getRoleByName(String roleName);
    Future<List<Role>> getRolesByUserId(Integer userId);
}
