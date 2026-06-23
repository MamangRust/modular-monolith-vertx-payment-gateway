package io.example.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.auth.model.AuthUser;
import io.example.auth.domain.requests.ResetPasswordRequest;
import io.example.auth.repository.ResetTokenRepository;
import io.example.auth.repository.UserRepository;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasswordResetService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final ResetTokenRepository resetTokenRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final KafkaService kafkaService;

    public Future<Boolean> forgotPassword(String email) {
        var ctx = tracingMetrics.startSpan("PasswordResetService.forgotPassword");

        return userRepository.findByEmail(email)
                .compose(user -> {
                    if (user == null)
                        return Future.failedFuture(new NotFoundException("User not found"));

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
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "forgotPassword", "Process completed"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "forgotPassword", err.getMessage()));
    }

    public Future<Boolean> resetPassword(ResetPasswordRequest request) {
        var ctx = tracingMetrics.startSpan("PasswordResetService.resetPassword");

        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            return Future.failedFuture(new BadRequestException("Passwords do not match"));
        }

        return redisService.get("resetToken:" + request.getResetToken())
                .compose(cachedUserId -> {
                    if (cachedUserId != null)
                        return Future.succeededFuture(Integer.parseInt(cachedUserId));
                    return resetTokenRepository.findByToken(request.getResetToken())
                            .compose(rt -> {
                                if (rt == null)
                                    return Future
                                            .failedFuture(new BadRequestException("Invalid or expired reset token"));
                                return Future.succeededFuture(rt.getUserId());
                            });
                })
                .compose(userId -> userRepository.updateUserPassword(userId, request.getPassword())
                        .compose(u -> {
                            resetTokenRepository.deleteResetToken(userId);
                            redisService.delete("resetToken:" + request.getResetToken());
                            return Future.succeededFuture(true);
                        }))
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "resetPassword", "Password reset successfully"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "resetPassword", err.getMessage()));
    }

    public Future<Boolean> verifyCode(String code) {
        var ctx = tracingMetrics.startSpan("PasswordResetService.verifyCode");

        return userRepository.findByVerificationCode(code)
                .compose(user -> {
                    if (user == null)
                        return Future.failedFuture(new BadRequestException("Invalid verification code"));

                    return userRepository.updateUserIsVerified(user.getUserId(), true)
                            .compose(u -> redisService.delete("verification:" + user.getEmail())
                                    .compose(v -> sendVerificationSuccessEmail(user)
                                            .recover(err -> {
                                                logger.warn("Failed to send verification success email", err);
                                                return Future.succeededFuture();
                                            }))
                                    .map(v -> true));
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "verifyCode", "Code verified successfully"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "verifyCode", err.getMessage()));
    }

    private Future<Void> sendForgotPasswordEmail(AuthUser user, String token) {
        if (kafkaService == null)
            return Future.succeededFuture();

        JsonObject emailPayload = new JsonObject()
                .put("email", user.getEmail())
                .put("subject", "Password Reset Request")
                .put("body", "Click to reset your password: https://sanedge.example.com/reset-password?token=" + token);

        return kafkaService.sendMessage("email-service-topic-auth-forgot-password", user.getUserId().toString(),
                emailPayload);
    }

    private Future<Void> sendVerificationSuccessEmail(AuthUser user) {
        if (kafkaService == null)
            return Future.succeededFuture();

        JsonObject emailPayload = new JsonObject()
                .put("email", user.getEmail())
                .put("subject", "Verification Success")
                .put("body", "Your account has been successfully verified.");

        return kafkaService.sendMessage("email-service-topic-auth-verify-code-success", user.getUserId().toString(),
                emailPayload);
    }
}