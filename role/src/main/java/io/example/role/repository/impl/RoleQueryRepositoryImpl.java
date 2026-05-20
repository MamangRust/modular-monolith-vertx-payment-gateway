package io.example.role.repository.impl;

import java.util.ArrayList;
import java.util.List;
import io.example.common.domain.PagedResult;
import io.example.role.model.Role;
import io.example.role.repository.RoleQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class RoleQueryRepositoryImpl implements RoleQueryRepository {
  private final Pool client;

  public RoleQueryRepositoryImpl(Pool client) {
    this.client = client;
  }

  @Override
  public Future<PagedResult<Role>> getRoles(String search, int page, int pageSize) {
    int offset = (page > 0 ? page - 1 : 0) * pageSize;
    return client
        .preparedQuery("""
            SELECT role_id, role_name, created_at, updated_at, deleted_at, COUNT(*) OVER() AS total_count
            FROM roles
            WHERE ($1::TEXT IS NULL OR role_name ILIKE '%' || $1 || '%')
            ORDER BY created_at ASC LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(normalizeSearch(search), pageSize, offset))
        .map(this::mapPagedRoles);
  }

  @Override
  public Future<PagedResult<Role>> getActiveRoles(String search, int page, int pageSize) {
    int offset = (page > 0 ? page - 1 : 0) * pageSize;
    return client
        .preparedQuery("""
            SELECT role_id, role_name, created_at, updated_at, deleted_at, COUNT(*) OVER() AS total_count
            FROM roles
            WHERE deleted_at IS NULL AND ($1::TEXT IS NULL OR role_name ILIKE '%' || $1 || '%')
            ORDER BY created_at ASC LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(normalizeSearch(search), pageSize, offset))
        .map(this::mapPagedRoles);
  }

  @Override
  public Future<PagedResult<Role>> getTrashedRoles(String search, int page, int pageSize) {
    int offset = (page > 0 ? page - 1 : 0) * pageSize;
    return client
        .preparedQuery("""
            SELECT role_id, role_name, created_at, updated_at, deleted_at, COUNT(*) OVER() AS total_count
            FROM roles
            WHERE deleted_at IS NOT NULL AND ($1::TEXT IS NULL OR role_name ILIKE '%' || $1 || '%')
            ORDER BY deleted_at DESC LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(normalizeSearch(search), pageSize, offset))
        .map(this::mapPagedRoles);
  }

  @Override
  public Future<Role> getRoleById(Integer roleId) {
    return client
        .preparedQuery("SELECT role_id, role_name, created_at, updated_at, deleted_at FROM roles WHERE role_id = $1 AND deleted_at IS NULL")
        .execute(Tuple.of(roleId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Role> getRoleByName(String roleName) {
    return client
        .preparedQuery("SELECT role_id, role_name, created_at, updated_at, deleted_at FROM roles WHERE role_name = $1 AND deleted_at IS NULL")
        .execute(Tuple.of(roleName))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<List<Role>> getRolesByUserId(Integer userId) {
    return client
        .preparedQuery("""
            SELECT r.role_id, r.role_name, r.created_at, r.updated_at, r.deleted_at
            FROM roles r
            JOIN user_roles ur ON r.role_id = ur.role_id
            WHERE ur.user_id = $1 AND r.deleted_at IS NULL AND ur.deleted_at IS NULL
            ORDER BY r.role_id ASC
            """)
        .execute(Tuple.of(userId))
        .map(rows -> {
          List<Role> list = new ArrayList<>();
          for (Row row : rows) {
            list.add(Role.fromRow(row));
          }
          return list;
        });
  }

  private String normalizeSearch(String search) {
    return (search == null || search.isBlank()) ? null : search;
  }

  private Role mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Role.fromRow(rows.iterator().next()) : null;
  }

  private PagedResult<Role> mapPagedRoles(RowSet<Row> rows) {
    List<Role> roles = new ArrayList<>();
    int total = 0;
    for (Row row : rows) {
      roles.add(Role.fromRow(row));
      if (total == 0) {
        Integer tc = row.getInteger("total_count");
        if (tc != null) total = tc;
      }
    }
    return new PagedResult<>(roles, total);
  }
}
