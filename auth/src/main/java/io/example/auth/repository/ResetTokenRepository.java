package io.example.auth.repository;

import io.example.auth.model.ResetToken;
import io.vertx.core.Future;

import java.time.LocalDateTime;

public interface ResetTokenRepository {
    Future<ResetToken> findByToken(String code);
    Future<ResetToken> createResetToken(Integer userId, String token, LocalDateTime expiredAt);
    Future<Void> deleteResetToken(Integer userId);
}
