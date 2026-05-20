package io.example.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.auth.model.AuthUser;
import io.example.auth.model.ResetPasswordRequest;
import io.example.auth.repository.ResetTokenRepository;
import io.example.auth.repository.UserRepository;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.common.service.KafkaService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class PasswordResetService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final ResetTokenRepository resetTokenRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final KafkaService kafkaService;

    public PasswordResetService(UserRepository userRepository,
            ResetTokenRepository resetTokenRepository,
            RedisService redisService,
            TracingMetrics tracingMetrics,
            KafkaService kafkaService) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
        this.kafkaService = kafkaService;
    }

    public Future<Boolean> forgotPassword(String email) {
        String method = "ForgotPassword";
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(method);

        return userRepository.findByEmail(email)
                .compose(user -> {
                    if (user == null) {
                        return Future.failedFuture("User not found");
                    }

                    String token = UUID.randomUUID().toString().substring(0, 10);
                    LocalDateTime expiry = LocalDateTime.now().plusHours(24);

                    return resetTokenRepository.createResetToken(user.getUserId(), token, expiry)
                            .compose(rt -> redisService
                                    .set("resetToken:" + token, user.getUserId().toString(), Duration.ofMinutes(5))
                                    .compose(v -> sendForgotPasswordEmail(user, token)
                                            .recover(err -> {
                                                logger.warn("Failed to send forgot password email", err);
                                                return Future.succeededFuture();
                                            }))
                                    .map(v -> true));
                })
                .onSuccess(
                        v -> tracingMetrics.completeSpanSuccess(tracingContext, method, "Forgot password email process completed"))
                .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, method, err.getMessage()));
    }

    public Future<Boolean> resetPassword(ResetPasswordRequest request) {
        String method = "ResetPassword";
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(method);

        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            return Future.failedFuture("Passwords do not match");
        }

        return redisService.get("resetToken:" + request.getResetToken())
                .compose(cachedUserId -> {
                    if (cachedUserId != null) {
                        return Future.succeededFuture(Integer.parseInt(cachedUserId));
                    }

                    return resetTokenRepository.findByToken(request.getResetToken())
                            .compose(rt -> {
                                if (rt == null) {
                                    return Future.failedFuture("Invalid or expired reset token");
                                }
                                return Future.succeededFuture(rt.getUserId());
                            });
                })
                .compose(userId -> userRepository.updateUserPassword(userId, request.getPassword())
                        .compose(u -> {
                            resetTokenRepository.deleteResetToken(userId);
                            redisService.delete("resetToken:" + request.getResetToken());
                            return Future.succeededFuture(true);
                        }))
                .onSuccess(
                        v -> tracingMetrics.completeSpanSuccess(tracingContext, method, "Password reset successfully"))
                .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, method, err.getMessage()));
    }

    public Future<Boolean> verifyCode(String code) {
        String method = "VerifyCode";
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(method);

        return userRepository.findByVerificationCode(code)
                .compose(user -> {
                    if (user == null) {
                        return Future.failedFuture("Invalid verification code");
                    }

                    return userRepository.updateUserIsVerified(user.getUserId(), true)
                            .compose(u -> redisService.delete("verification:" + user.getEmail())
                                    .compose(v -> sendVerificationSuccessEmail(user)
                                            .recover(err -> {
                                                logger.warn("Failed to send verification success email", err);
                                                return Future.succeededFuture();
                                            }))
                                    .map(v -> true));
                })
                .onSuccess(
                        v -> tracingMetrics.completeSpanSuccess(tracingContext, method, "Code verified successfully"))
                .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, method, err.getMessage()));
    }

    private Future<Void> sendForgotPasswordEmail(AuthUser user, String token) {
        if (kafkaService == null) {
            logger.warn("Kafka service not initialized, skipping forgot password email for {}", user.getEmail());
            return Future.succeededFuture();
        }

        JsonObject emailPayload = new JsonObject()
                .put("email", user.getEmail())
                .put("subject", "Password Reset Request")
                .put("body", "Click to reset your password: https://sanedge.example.com/reset-password?token=" + token);

        return kafkaService.sendMessage("email-service-topic-auth-forgot-password", user.getUserId().toString(),
                emailPayload);
    }

    private Future<Void> sendVerificationSuccessEmail(AuthUser user) {
        if (kafkaService == null) {
            logger.warn("Kafka service not initialized, skipping verification success email for {}", user.getEmail());
            return Future.succeededFuture();
        }

        JsonObject emailPayload = new JsonObject()
                .put("email", user.getEmail())
                .put("subject", "Verification Success")
                .put("body", "Your account has been successfully verified.");

        return kafkaService.sendMessage("email-service-topic-auth-verify-code-success", user.getUserId().toString(),
                emailPayload);
    }
}
