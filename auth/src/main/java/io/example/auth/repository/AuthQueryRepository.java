package io.example.auth.repository;

import io.example.auth.model.AuthUser;
import io.example.auth.model.RefreshToken;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

public class AuthQueryRepository {
  private final Pool pool;

  public AuthQueryRepository(Pool pool) {
    this.pool = pool;
  }

  public Future<AuthUser> getUserByEmailWithRoles(String email) {
    return pool.preparedQuery("""
            SELECT
              u.user_id, u.firstname, u.lastname, u.email, u.password, u.created_at, u.updated_at, u.deleted_at,
              r.role_name
            FROM users u
            LEFT JOIN user_roles ur ON u.user_id = ur.user_id AND ur.deleted_at IS NULL
            LEFT JOIN roles r ON ur.role_id = r.role_id AND r.deleted_at IS NULL
            WHERE u.email = $1 AND u.deleted_at IS NULL
            """)
        .execute(Tuple.of(email))
        .map(AuthUser::fromRowsWithRoles);
  }

  public Future<AuthUser> getUserByIdWithRoles(Integer userId) {
    return pool.preparedQuery("""
            SELECT
              u.user_id, u.firstname, u.lastname, u.email, u.password, u.created_at, u.updated_at, u.deleted_at,
              r.role_name
            FROM users u
            LEFT JOIN user_roles ur ON u.user_id = ur.user_id AND ur.deleted_at IS NULL
            LEFT JOIN roles r ON ur.role_id = r.role_id AND r.deleted_at IS NULL
            WHERE u.user_id = $1 AND u.deleted_at IS NULL
            """)
        .execute(Tuple.of(userId))
        .map(AuthUser::fromRowsWithRoles);
  }

  public Future<RefreshToken> findRefreshTokenByToken(String token) {
    return pool.preparedQuery("""
            SELECT refresh_token_id, user_id, token, expiration, created_at, updated_at, deleted_at
            FROM refresh_tokens
            WHERE token = $1 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(token))
        .map(rows -> rows.iterator().hasNext() ? RefreshToken.fromRow(rows.iterator().next()) : null);
  }
}
