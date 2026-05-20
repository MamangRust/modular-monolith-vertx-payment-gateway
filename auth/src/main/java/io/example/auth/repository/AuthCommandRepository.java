package io.example.auth.repository;

import java.time.LocalDateTime;
import io.example.auth.model.AuthUser;
import io.example.auth.model.RefreshToken;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

public class AuthCommandRepository {
  private final Pool pool;

  public AuthCommandRepository(Pool pool) {
    this.pool = pool;
  }

  public Future<AuthUser> createUser(String firstname, String lastname, String email, String password) {
    return pool.preparedQuery("""
            INSERT INTO users (firstname, lastname, email, password)
            VALUES ($1, $2, $3, $4)
            RETURNING user_id, firstname, lastname, email, password, created_at, updated_at, deleted_at
            """)
        .execute(Tuple.of(firstname, lastname, email, password))
        .map(rows -> AuthUser.fromRow(rows.iterator().next()));
  }

  public Future<Void> assignDefaultAdminRole(Integer userId) {
    return pool.preparedQuery("""
            INSERT INTO user_roles (user_id, role_id)
            SELECT $1, role_id FROM roles WHERE role_name = 'admin' LIMIT 1
            ON CONFLICT DO NOTHING
            """)
        .execute(Tuple.of(userId))
        .mapEmpty();
  }

  public Future<RefreshToken> createRefreshToken(Integer userId, String token, LocalDateTime expiration) {
    return pool.preparedQuery("""
            INSERT INTO refresh_tokens (user_id, token, expiration, created_at, updated_at)
            VALUES ($1, $2, $3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING refresh_token_id, user_id, token, expiration, created_at, updated_at, deleted_at
            """)
        .execute(Tuple.of(userId, token, expiration))
        .map(rows -> rows.iterator().hasNext() ? RefreshToken.fromRow(rows.iterator().next()) : null);
  }

  public Future<Void> deleteRefreshTokenByUserId(Integer userId) {
    return pool.preparedQuery("""
            DELETE FROM refresh_tokens
            WHERE user_id = $1
            """)
        .execute(Tuple.of(userId))
        .mapEmpty();
  }
}
