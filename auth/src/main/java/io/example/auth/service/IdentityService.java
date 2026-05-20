package io.example.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.auth.model.AuthUser;
import io.example.auth.model.TokenResponse;
import io.example.auth.repository.RefreshTokenRepository;
import io.example.auth.repository.UserRepository;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;

public class IdentityService {
    private static final Logger logger = LoggerFactory.getLogger(IdentityService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisService redisService;
    private final TokenService tokenService;
    private final JWTAuth jwtProvider;
    private final TracingMetrics tracingMetrics;

    public IdentityService(UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            RedisService redisService,
            TokenService tokenService,
            JWTAuth jwtProvider,
            TracingMetrics tracingMetrics) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.redisService = redisService;
        this.tokenService = tokenService;
        this.jwtProvider = jwtProvider;
        this.tracingMetrics = tracingMetrics;
    }

    public Future<TokenResponse> refreshToken(String token) {
        String method = "RefreshToken";
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(method);

        return redisService.get("refreshToken:" + token)
                .<TokenResponse>compose(cachedUserId -> {
                    if (cachedUserId != null) {
                        Integer userId = Integer.parseInt(cachedUserId);
                        redisService.delete("refreshToken:" + token);
                        logger.debug("Invalidated old refresh token from cache: {}", token);

                        String newAccessToken = tokenService.createAccessToken(userId);
                        String newRefreshToken = tokenService.createRefreshToken(userId);

                        return redisService
                                .set("refreshToken:" + newRefreshToken, userId.toString(), Duration.ofHours(24))
                                .map(v -> TokenResponse.builder()
                                        .accessToken(newAccessToken)
                                        .refreshToken(newRefreshToken)
                                        .build());
                    }

                    // If not in cache, validate JWT
                    return jwtProvider.authenticate(new TokenCredentials(token))
                            .<TokenResponse>compose(user -> {
                                final Integer userId = Integer.parseInt(user.principal().getString("sub"));

                                return refreshTokenRepository.deleteRefreshToken(token)
                                        .compose(v -> {
                                            String newAccessToken = tokenService.createAccessToken(userId);
                                            String newRefreshToken = tokenService.createRefreshToken(userId);
                                            LocalDateTime expiryTime = LocalDateTime.now().plusHours(24);

                                            return refreshTokenRepository
                                                    .updateRefreshToken(userId, newRefreshToken, expiryTime)
                                                    .compose(rt -> {
                                                        return redisService
                                                                .set("refreshToken:" + newRefreshToken,
                                                                        userId.toString(), Duration.ofHours(24))
                                                                .map(v2 -> TokenResponse.builder()
                                                                        .accessToken(newAccessToken)
                                                                        .refreshToken(newRefreshToken)
                                                                        .build());
                                                    });
                                        });
                            });
                })
                .onSuccess(res -> tracingMetrics.completeSpanSuccess(tracingContext, method,
                        "Token refreshed successfully"))
                .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, method, err.getMessage()));
    }

    public Future<AuthUser> getMe(Integer userId) {
        String method = "GetMe";
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(method);

        return redisService.getJson("user:" + userId, AuthUser.class)
                .compose(cachedUser -> {
                    if (cachedUser != null) {
                        return Future.succeededFuture(cachedUser);
                    }

                    return userRepository.findById(userId)
                            .compose(user -> {
                                if (user == null) {
                                    return Future.failedFuture("User not found");
                                }
                                return redisService.setJson("user:" + userId, user, Duration.ofMinutes(5))
                                        .map(v -> user);
                            });
                })
                .onSuccess(res -> tracingMetrics.completeSpanSuccess(tracingContext, method,
                        "User details fetched successfully"))
                .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, method, err.getMessage()));
    }
}
