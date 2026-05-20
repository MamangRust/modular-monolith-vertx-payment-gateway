package io.example.auth.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.example.auth.model.AuthRequest;
import io.example.auth.model.TokenResponse;
import io.example.auth.repository.UserRepository;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;

public class LoginService {
    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

    private final UserRepository userRepository;
    private final RedisService redisService;
    private final TokenService tokenService;
    private final TracingMetrics tracingMetrics;

    public LoginService(UserRepository userRepository,
            RedisService redisService,
            TokenService tokenService,
            TracingMetrics tracingMetrics) {
        this.userRepository = userRepository;
        this.redisService = redisService;
        this.tokenService = tokenService;
        this.tracingMetrics = tracingMetrics;
    }

    public Future<TokenResponse> login(AuthRequest request) {
        String method = "Login";
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(method);

        String lockKey = "account_locked:" + request.getEmail();
        String failedAttemptsKey = "failed_login:" + request.getEmail();

        return redisService.exists(lockKey)
                .compose(locked -> {
                    if (locked) {
                        return Future.failedFuture("Account is locked due to too many failed attempts");
                    }

                    return userRepository.findByEmailAndVerify(request.getEmail());
                })
                .compose(user -> {
                    if (user == null) {
                        return Future.failedFuture("User not found or not verified");
                    }

                    BCrypt.Result result = BCrypt.verifyer().verify(request.getPassword().toCharArray(),
                            user.getPassword());
                    if (!result.verified) {
                        return redisService.incr(failedAttemptsKey)
                                .compose(attempts -> {
                                    if (attempts >= 5) {
                                        return redisService.set(lockKey, "true", Duration.ofHours(1))
                                                .compose(v -> Future
                                                        .failedFuture("Account locked due to 5 failed attempts"));
                                    }
                                    // Set TTL for failed attempts if it's new
                                    if (attempts == 1) {
                                        redisService.expire(failedAttemptsKey, Duration.ofMinutes(15));
                                    }
                                    return Future.failedFuture("Invalid password");
                                });
                    }

                    // Success
                    String accessToken = tokenService.createAccessToken(user.getUserId());
                    String refreshToken = tokenService.createRefreshToken(user.getUserId());

                    // Reset failed attempts
                    redisService.delete(failedAttemptsKey);
                    redisService.delete(lockKey);

                    return Future.succeededFuture(TokenResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .build());
                })
                .onSuccess(res -> tracingMetrics.completeSpanSuccess(tracingContext, method, "Login successful"))
                .onFailure(err -> {
                    logger.error("Login failed for {}: {}", request.getEmail(), err.getMessage());
                    tracingMetrics.completeSpanError(tracingContext, method, err.getMessage());
                });
    }
}
