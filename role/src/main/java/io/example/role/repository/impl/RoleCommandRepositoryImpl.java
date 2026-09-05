package io.example.role.repository.impl;

import io.example.role.domain.requests.CreateRoleRequest;
import io.example.role.domain.requests.UpdateRoleRequest;
import io.example.role.model.Role;
import io.example.role.repository.RoleCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class RoleCommandRepositoryImpl implements RoleCommandRepository {
  private final Pool client;

  public RoleCommandRepositoryImpl(Pool client) {
    this.client = client;
  }

  @Override
  public Future<Role> createRole(CreateRoleRequest request) {
    return client
        .preparedQuery(
            "INSERT INTO roles (role_name) VALUES ($1) RETURNING role_id, role_name, created_at, updated_at, deleted_at")
        .execute(Tuple.of(request.getName()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Role> updateRole(UpdateRoleRequest request) {
    return client
        .preparedQuery(
            "UPDATE roles SET role_name = COALESCE(NULLIF($1, ''), role_name), updated_at = CURRENT_TIMESTAMP WHERE role_id = $2 AND deleted_at IS NULL RETURNING role_id, role_name, created_at, updated_at, deleted_at")
        .execute(Tuple.of(request.getName() != null ? request.getName() : "", request.getRoleId()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Role> trashed(Integer roleId) {
    return client
        .preparedQuery(
            "UPDATE roles SET deleted_at = CURRENT_TIMESTAMP WHERE role_id = $1 AND deleted_at IS NULL RETURNING role_id, role_name, created_at, updated_at, deleted_at")
        .execute(Tuple.of(roleId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Role> restore(Integer roleId) {
    return client
        .preparedQuery(
            "UPDATE roles SET deleted_at = null WHERE role_id = $1 RETURNING role_id, role_name, created_at, updated_at, deleted_at")
        .execute(Tuple.of(roleId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Boolean> deletePermanent(Integer roleId) {
    return client
        .preparedQuery("DELETE FROM roles WHERE role_id = $1")
        .execute(Tuple.of(roleId))
        .map(rows -> rows.rowCount() > 0);
  }

  @Override
  public Future<Integer> restoreAllRoles() {
    return client.query("UPDATE roles SET deleted_at = NULL WHERE deleted_at IS NOT NULL").execute()
        .map(RowSet::rowCount);
  }

  @Override
  public Future<Integer> deleteAllPermanentRoles() {
    return client.query("DELETE FROM roles WHERE deleted_at IS NOT NULL").execute().map(RowSet::rowCount);
  }

  private Role mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Role.fromRow(rows.iterator().next()) : null;
  }
}
