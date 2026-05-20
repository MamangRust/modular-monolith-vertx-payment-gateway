package io.example.user.repository.impl;

import java.util.ArrayList;
import java.util.List;
import io.example.common.domain.PagedResult;
import io.example.user.model.User;
import io.example.user.repository.UserQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class UserQueryRepositoryImpl implements UserQueryRepository {
  private final Pool client;

  public UserQueryRepositoryImpl(Pool client) {
    this.client = client;
  }

  @Override
  public Future<PagedResult<User>> getUsers(String search, int page, int pageSize) {
    int offset = (page > 0 ? page - 1 : 0) * pageSize;
    return client
        .preparedQuery("""
            SELECT *, COUNT(*) OVER() AS total_count
            FROM users
            WHERE deleted_at IS NULL
              AND ($1::TEXT IS NULL OR firstname ILIKE '%' || $1 || '%' OR lastname ILIKE '%' || $1 || '%' OR email ILIKE '%' || $1 || '%')
            ORDER BY created_at DESC LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(normalizeSearch(search), pageSize, offset))
        .map(this::mapPagedUsers);
  }

  @Override
  public Future<PagedResult<User>> getActiveUsers(String search, int page, int pageSize) {
    int offset = (page > 0 ? page - 1 : 0) * pageSize;
    return client
        .preparedQuery("""
            SELECT *, COUNT(*) OVER() AS total_count
            FROM users
            WHERE deleted_at IS NULL
              AND ($1::TEXT IS NULL OR firstname ILIKE '%' || $1 || '%' OR lastname ILIKE '%' || $1 || '%' OR email ILIKE '%' || $1 || '%')
            ORDER BY created_at DESC LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(normalizeSearch(search), pageSize, offset))
        .map(this::mapPagedUsers);
  }

  @Override
  public Future<PagedResult<User>> getTrashedUsers(String search, int page, int pageSize) {
    int offset = (page > 0 ? page - 1 : 0) * pageSize;
    return client
        .preparedQuery("""
            SELECT *, COUNT(*) OVER() AS total_count
            FROM users
            WHERE deleted_at IS NOT NULL
              AND ($1::TEXT IS NULL OR firstname ILIKE '%' || $1 || '%' OR lastname ILIKE '%' || $1 || '%' OR email ILIKE '%' || $1 || '%')
            ORDER BY created_at DESC LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(normalizeSearch(search), pageSize, offset))
        .map(this::mapPagedUsers);
  }

  @Override
  public Future<User> getUserById(Integer userId) {
    return client
        .preparedQuery("SELECT user_id, firstname, lastname, email, password, created_at, updated_at, deleted_at FROM users WHERE user_id = $1 AND deleted_at IS NULL")
        .execute(Tuple.of(userId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<User> getUserByEmail(String email) {
    return client
        .preparedQuery("SELECT user_id, firstname, lastname, email, password, created_at, updated_at, deleted_at FROM users WHERE email = $1 AND deleted_at IS NULL")
        .execute(Tuple.of(email))
        .map(this::mapSingleOrNull);
  }

  private String normalizeSearch(String search) {
    return (search == null || search.isBlank()) ? null : search;
  }

  private User mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? User.fromRow(rows.iterator().next()) : null;
  }

  private PagedResult<User> mapPagedUsers(RowSet<Row> rows) {
    List<User> users = new ArrayList<>();
    int total = 0;
    for (Row row : rows) {
      users.add(User.fromRow(row));
      if (total == 0) {
        Integer tc = row.getInteger("total_count");
        if (tc != null) total = tc;
      }
    }
    return new PagedResult<>(users, total);
  }
}
