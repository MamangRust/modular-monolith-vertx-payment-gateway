package io.example.auth.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.example.auth.domain.requests.AuthRequest;
import io.example.auth.model.TokenResponse;
import io.example.auth.repository.UserRepository;
import io.example.common.exception.grpc.ForbiddenException;
import io.example.common.exception.grpc.UnauthorizedException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginService {
    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

    private final UserRepository userRepository;
    private final RedisService redisService;
    private final TokenService tokenService;
    private final TracingMetrics tracingMetrics;

    public Future<TokenResponse> login(AuthRequest request) {
        var ctx = tracingMetrics.startSpan("LoginService.login");

        String lockKey = "account_locked:" + request.getEmail();
        String failedAttemptsKey = "failed_login:" + request.getEmail();

        return redisService.exists(lockKey)
                .compose(locked -> {
                    if (locked) {
                        return Future.failedFuture(
                                new ForbiddenException("Account is locked due to too many failed attempts"));
                    }
                    return userRepository.findByEmailAndVerify(request.getEmail());
                })
                .compose(user -> {
                    if (user == null) {
                        return Future.failedFuture(new UnauthorizedException("Invalid credentials"));
                    }

                    BCrypt.Result result = BCrypt.verifyer().verify(request.getPassword().toCharArray(),
                            user.getPassword());
                    if (!result.verified) {
                        return redisService.incr(failedAttemptsKey)
                                .compose(attempts -> {
                                    if (attempts >= 5) {
                                        return redisService.set(lockKey, "true", Duration.ofHours(1))
                                                .compose(v -> Future.failedFuture(new ForbiddenException(
                                                        "Account locked due to 5 failed attempts")));
                                    }
                                    if (attempts == 1) {
                                        redisService.expire(failedAttemptsKey, Duration.ofMinutes(15));
                                    }
                                    return Future.failedFuture(new UnauthorizedException("Invalid credentials"));
                                });
                    }

                    String accessToken = tokenService.createAccessToken(user.getUserId(), user.getRoles());
                    String refreshToken = tokenService.createRefreshToken(user.getUserId());

                    redisService.delete(failedAttemptsKey);
                    redisService.delete(lockKey);

                    return Future.succeededFuture(TokenResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .build());
                })
                .onSuccess(res -> tracingMetrics.completeSpanSuccess(ctx, "login", "Login successful"))
                .onFailure(err -> {
                    logger.warn("Login failed for {}: {}", request.getEmail(), err.getMessage());
                    tracingMetrics.completeSpanError(ctx, "login", err.getMessage());
                });
    }
}