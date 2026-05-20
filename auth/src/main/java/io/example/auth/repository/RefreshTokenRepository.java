package io.example.auth.repository;

import io.example.auth.model.RefreshToken;
import io.vertx.core.Future;

import java.time.LocalDateTime;

public interface RefreshTokenRepository {
    Future<RefreshToken> findByToken(String token);
    Future<RefreshToken> findByUserId(Integer userId);
    Future<RefreshToken> createRefreshToken(Integer userId, String token, LocalDateTime expiresAt);
    Future<RefreshToken> updateRefreshToken(Integer userId, String token, LocalDateTime expiresAt);
    Future<Void> deleteRefreshToken(String token);
    Future<Void> deleteRefreshTokenByUserId(Integer userId);
}
