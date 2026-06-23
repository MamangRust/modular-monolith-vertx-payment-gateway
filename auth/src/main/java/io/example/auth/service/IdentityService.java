package io.example.auth.service;

import java.time.Duration;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.auth.model.AuthUser;
import io.example.auth.model.TokenResponse;
import io.example.auth.repository.RefreshTokenRepository;
import io.example.auth.repository.UserRepository;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.exception.grpc.UnauthorizedException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IdentityService {
        private static final Logger logger = LoggerFactory.getLogger(IdentityService.class);

        private final UserRepository userRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final RedisService redisService;
        private final TokenService tokenService;
        private final JWTAuth jwtProvider;
        private final TracingMetrics tracingMetrics;

        public Future<TokenResponse> refreshToken(String token) {
                var ctx = tracingMetrics.startSpan("IdentityService.refreshToken");
                Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

                return redisService.get("refreshToken:" + token)
                                .<TokenResponse>compose(cachedUserId -> {
                                        if (cachedUserId != null) {
                                                span.setAttribute("auth.refresh_cache_hit", true);
                                                Integer userId = Integer.parseInt(cachedUserId);
                                                logger.debug("Refresh token found in cache for userId: {}", userId);

                                                redisService.delete("refreshToken:" + token);
                                                logger.debug("Invalidated old refresh token from cache: {}", token);

                                                String newAccessToken = tokenService.createAccessToken(userId);
                                                String newRefreshToken = tokenService.createRefreshToken(userId);

                                                return redisService
                                                                .set("refreshToken:" + newRefreshToken,
                                                                                userId.toString(), Duration.ofHours(24))
                                                                .map(v -> TokenResponse.builder()
                                                                                .accessToken(newAccessToken)
                                                                                .refreshToken(newRefreshToken)
                                                                                .build());
                                        }

                                        span.setAttribute("auth.refresh_cache_hit", false);
                                        logger.debug("Refresh token not found in cache, validating via JWT: {}", token);

                                        return jwtProvider.authenticate(new TokenCredentials(token))
                                                        .recover(err -> {
                                                                logger.warn("JWT authentication failed for refresh token: {}",
                                                                                err.getMessage());
                                                                return Future.failedFuture(new UnauthorizedException(
                                                                                "Invalid or expired refresh token"));
                                                        })
                                                        .<TokenResponse>compose(user -> {
                                                                final Integer userId = Integer.parseInt(
                                                                                user.principal().getString("sub"));
                                                                logger.info("Rotating refresh token in database for userId: {}",
                                                                                userId);

                                                                return refreshTokenRepository.deleteRefreshToken(token)
                                                                                .compose(v -> {
                                                                                        String newAccessToken = tokenService
                                                                                                        .createAccessToken(
                                                                                                                        userId);
                                                                                        String newRefreshToken = tokenService
                                                                                                        .createRefreshToken(
                                                                                                                        userId);
                                                                                        var expiryTime = java.time.LocalDateTime
                                                                                                        .now()
                                                                                                        .plusHours(24);

                                                                                        return refreshTokenRepository
                                                                                                        .updateRefreshToken(
                                                                                                                        userId,
                                                                                                                        newRefreshToken,
                                                                                                                        expiryTime)
                                                                                                        .compose(rt -> redisService
                                                                                                                        .set("refreshToken:"
                                                                                                                                        + newRefreshToken,
                                                                                                                                        userId.toString(),
                                                                                                                                        Duration.ofHours(
                                                                                                                                                        24))
                                                                                                                        .map(v2 -> TokenResponse
                                                                                                                                        .builder()
                                                                                                                                        .accessToken(newAccessToken)
                                                                                                                                        .refreshToken(newRefreshToken)
                                                                                                                                        .build()));
                                                                                });
                                                        });
                                })
                                .onSuccess(res -> tracingMetrics.completeSpanSuccess(ctx, "refreshToken",
                                                "Token refreshed successfully"))
                                .onFailure(err -> {
                                        logger.error("Failed to refresh token: {}", err.getMessage());
                                        tracingMetrics.completeSpanError(ctx, "refreshToken", err.getMessage());
                                });
        }

        public Future<AuthUser> getMe(Integer userId) {
                var ctx = tracingMetrics.startSpan("IdentityService.getMe",
                                io.opentelemetry.api.common.Attributes.builder().put("user.id", userId).build());
                Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));

                return redisService.getJson("user:" + userId, AuthUser.class)
                                .compose(cachedUser -> {
                                        if (cachedUser != null) {
                                                span.setAttribute("auth.user_cache_hit", true);
                                                logger.debug("User fetched from cache for userId: {}", userId);
                                                return Future.succeededFuture(cachedUser);
                                        }
                                        span.setAttribute("auth.user_cache_hit", false);
                                        logger.debug("User not found in cache, fetching from DB for userId: {}",
                                                        userId);

                                        return userRepository.findById(userId)
                                                        .compose(user -> {
                                                                if (user == null) {
                                                                        logger.warn("User not found in DB for userId: {}",
                                                                                        userId);
                                                                        return Future.failedFuture(
                                                                                        new NotFoundException(
                                                                                                        "User not found"));
                                                                }
                                                                return redisService
                                                                                .setJson("user:" + userId, user,
                                                                                                Duration.ofMinutes(5))
                                                                                .map(v -> user);
                                                        });
                                })
                                .onSuccess(res -> tracingMetrics.completeSpanSuccess(ctx, "getMe",
                                                "User details fetched successfully"))
                                .onFailure(err -> {
                                        logger.error("Failed to fetch user details for userId {}: {}", userId,
                                                        err.getMessage());
                                        tracingMetrics.completeSpanError(ctx, "getMe", err.getMessage());
                                });
        }
}