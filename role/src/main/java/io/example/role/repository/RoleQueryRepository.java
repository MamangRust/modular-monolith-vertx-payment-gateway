package io.example.role.repository;

import java.util.List;
import io.example.common.domain.PagedResult;
import io.example.role.domain.requests.FindAllRoles;
import io.example.role.model.Role;
import io.vertx.core.Future;

public interface RoleQueryRepository {
    Future<PagedResult<Role>> getRoles(FindAllRoles request);

    Future<PagedResult<Role>> getActiveRoles(FindAllRoles request);

    Future<PagedResult<Role>> getTrashedRoles(FindAllRoles request);

    Future<Role> getRoleById(Integer roleId);

    Future<Role> findByTrashedId(Integer roleId);

    Future<Role> getRoleByName(String roleName);

    Future<List<Role>> getRolesByUserId(Integer userId);
}
