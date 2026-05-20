package io.example.auth.repository.impl;

import io.example.auth.model.RefreshToken;
import io.example.auth.repository.RefreshTokenRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDateTime;

public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    private final Pool pool;

    public RefreshTokenRepositoryImpl(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<RefreshToken> findByToken(String token) {
        return pool.preparedQuery("""
                SELECT refresh_token_id, user_id, token, expiration, created_at, updated_at, deleted_at
                FROM refresh_tokens
                WHERE token = $1 AND deleted_at IS NULL
                """)
                .execute(Tuple.of(token))
                .map(rows -> rows.iterator().hasNext() ? RefreshToken.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<RefreshToken> findByUserId(Integer userId) {
        return pool.preparedQuery("""
                SELECT refresh_token_id, user_id, token, expiration, created_at, updated_at, deleted_at
                FROM refresh_tokens
                WHERE user_id = $1 AND deleted_at IS NULL
                ORDER BY created_at DESC
                LIMIT 1
                """)
                .execute(Tuple.of(userId))
                .map(rows -> rows.iterator().hasNext() ? RefreshToken.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<RefreshToken> createRefreshToken(Integer userId, String token, LocalDateTime expiresAt) {
        return pool.preparedQuery("""
                INSERT INTO refresh_tokens (user_id, token, expiration, created_at, updated_at)
                VALUES ($1, $2, $3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING refresh_token_id, user_id, token, expiration, created_at, updated_at, deleted_at
                """)
                .execute(Tuple.of(userId, token, expiresAt))
                .map(rows -> RefreshToken.fromRow(rows.iterator().next()));
    }

    @Override
    public Future<RefreshToken> updateRefreshToken(Integer userId, String token, LocalDateTime expiresAt) {
        return pool.preparedQuery("""
                UPDATE refresh_tokens
                SET token = $1, expiration = $2, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = $3 AND deleted_at IS NULL
                RETURNING *
                """)
                .execute(Tuple.of(token, expiresAt, userId))
                .map(rows -> rows.iterator().hasNext() ? RefreshToken.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Void> deleteRefreshToken(String token) {
        return pool.preparedQuery("DELETE FROM refresh_tokens WHERE token = $1")
                .execute(Tuple.of(token))
                .mapEmpty();
    }

    @Override
    public Future<Void> deleteRefreshTokenByUserId(Integer userId) {
        return pool.preparedQuery("DELETE FROM refresh_tokens WHERE user_id = $1")
                .execute(Tuple.of(userId))
                .mapEmpty();
    }
}
