package io.example.user.repository.impl;

import io.example.user.domain.requests.CreateUserRequest;
import io.example.user.domain.requests.UpdatePasswordRequest;
import io.example.user.domain.requests.UpdateUserRequest;
import io.example.user.model.User;
import io.example.user.repository.UserCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserCommandRepositoryImpl implements UserCommandRepository {
  private final Pool client;

  @Override
  public Future<User> createUser(CreateUserRequest request) {
    return client
        .preparedQuery(
            "INSERT INTO users (firstname, lastname, email, password) VALUES ($1, $2, $3, $4) RETURNING user_id, firstname, lastname, email, password, created_at, updated_at, deleted_at")
        .execute(Tuple.of(request.getFirstName(), request.getLastName(), request.getEmail(), request.getPassword()))
        .map(rows -> User.fromRow(rows.iterator().next()));
  }

  @Override
  public Future<Void> assignDefaultAdminRole(Integer userId) {
    return client
        .preparedQuery(
            "INSERT INTO user_roles (user_id, role_id) SELECT $1, role_id FROM roles WHERE role_name = 'ADMIN' LIMIT 1")
        .execute(Tuple.of(userId))
        .mapEmpty();
  }

  @Override
  public Future<User> updateUser(UpdateUserRequest request) {
    return client
        .preparedQuery(
            "UPDATE users SET firstname = $1, lastname = $2, email = $3, updated_at = CURRENT_TIMESTAMP WHERE user_id = $4 AND deleted_at IS NULL RETURNING user_id, firstname, lastname, email, password, created_at, updated_at, deleted_at")
        .execute(Tuple.of(request.getFirstName(), request.getLastName(), request.getEmail(), request.getUserId()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<User> updatePassword(UpdatePasswordRequest request) {
    return client
        .preparedQuery(
            "UPDATE users SET password = $1, updated_at = CURRENT_TIMESTAMP WHERE user_id = $2 AND deleted_at IS NULL RETURNING user_id, firstname, lastname, email, password, created_at, updated_at, deleted_at")
        .execute(Tuple.of(request.getPassword(), request.getUserId()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<User> restore(Integer userId) {
    return client
        .preparedQuery(
            "UPDATE users SET deleted_at = null WHERE user_id = $1 RETURNING user_id, firstname, lastname, email, password, created_at, updated_at, deleted_at")
        .execute(Tuple.of(userId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<User> trashed(Integer userId) {
    return client
        .preparedQuery(
            "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE user_id = $1 AND deleted_at IS NULL RETURNING user_id, firstname, lastname, email, password, created_at, updated_at, deleted_at")
        .execute(Tuple.of(userId))
        .map(this::mapSingleOrNull);
  }

  public Future<Boolean> deletePermanent(Integer userId) {
    return client
        .preparedQuery("DELETE FROM users WHERE user_id = $1 AND deleted_at IS NOT NULL")
        .execute(Tuple.of(userId))
        .map(rowSet -> rowSet.rowCount() > 0);
  }

  public Future<Integer> restoreAllUsers() {
    return client
        .query("UPDATE users SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
        .execute()
        .map(RowSet::rowCount);
  }

  public Future<Integer> deleteAllPermanentUsers() {
    return client
        .query("DELETE FROM users WHERE deleted_at IS NOT NULL")
        .execute()
        .map(RowSet::rowCount);
  }

  private User mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? User.fromRow(rows.iterator().next()) : null;
  }
}
