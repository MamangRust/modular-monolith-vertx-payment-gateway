package io.example.auth.repository.impl;

import io.example.auth.model.ResetToken;
import io.example.auth.repository.ResetTokenRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDateTime;

public class ResetTokenRepositoryImpl implements ResetTokenRepository {
    private final Pool pool;

    public ResetTokenRepositoryImpl(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<ResetToken> findByToken(String code) {
        return pool.preparedQuery("SELECT user_id, token, expiry_date FROM reset_tokens WHERE token = $1")
                .execute(Tuple.of(code))
                .map(rows -> rows.iterator().hasNext() ? ResetToken.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<ResetToken> createResetToken(Integer userId, String token, LocalDateTime expiredAt) {
        return pool.preparedQuery("""
                INSERT INTO reset_tokens (user_id, token, expiry_date)
                VALUES ($1, $2, $3)
                RETURNING user_id, token, expiry_date
                """)
                .execute(Tuple.of(userId, token, expiredAt))
                .map(rows -> ResetToken.fromRow(rows.iterator().next()));
    }

    @Override
    public Future<Void> deleteResetToken(Integer userId) {
        return pool.preparedQuery("DELETE FROM reset_tokens WHERE user_id = $1")
                .execute(Tuple.of(userId))
                .mapEmpty();
    }
}
