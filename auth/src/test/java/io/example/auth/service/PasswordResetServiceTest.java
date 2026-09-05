package io.example.auth.service;

import io.example.auth.domain.requests.ResetPasswordRequest;
import io.example.auth.model.AuthUser;
import io.example.auth.model.ResetToken;
import io.example.auth.repository.ResetTokenRepository;
import io.example.auth.repository.UserRepository;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class PasswordResetServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ResetTokenRepository resetTokenRepository;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics tracingMetrics;

  @Mock
  private KafkaService kafkaService;

  private PasswordResetService service;

  @BeforeEach
  void setUp() {
    service = new PasswordResetService(userRepository, resetTokenRepository, redisService,
        tracingMetrics, kafkaService);
  }

  private void mockTracing() {
    when(tracingMetrics.startSpan(anyString())).thenReturn(new TracingContext(null, null));
  }

  private AuthUser aUser() {
    return AuthUser.builder()
        .userId(1)
        .firstname("Alice")
        .lastname("Wonderland")
        .email("alice@example.com")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /* ─── forgotPassword ─── */

  @Test
  @DisplayName("forgotPassword creates token, caches it, and sends email")
  void forgotPasswordSuccess(VertxTestContext ctx) {
    mockTracing();
    var user = aUser();

    when(userRepository.findByEmail("alice@example.com")).thenReturn(Future.succeededFuture(user));
    when(resetTokenRepository.createResetToken(any(), anyString(), any(LocalDateTime.class)))
        .thenReturn(Future.succeededFuture(new ResetToken(1, "token123", LocalDateTime.now().plusHours(24))));
    when(redisService.set(anyString(), anyString(), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));
    when(kafkaService.sendMessage(anyString(), anyString(), any())).thenReturn(Future.succeededFuture());

    service.forgotPassword("alice@example.com")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isTrue();
          verify(kafkaService).sendMessage(eq("email-service-topic-auth-forgot-password"), eq("1"), any());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("forgotPassword fails when user not found")
  void forgotPasswordUserNotFound(VertxTestContext ctx) {
    mockTracing();

    when(userRepository.findByEmail("missing@example.com")).thenReturn(Future.succeededFuture(null));

    service.forgotPassword("missing@example.com")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("User not found");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("forgotPassword succeeds even when email fails")
  void forgotPasswordEmailFailureTolerated(VertxTestContext ctx) {
    mockTracing();
    var user = aUser();

    when(userRepository.findByEmail("alice@example.com")).thenReturn(Future.succeededFuture(user));
    when(resetTokenRepository.createResetToken(any(), anyString(), any(LocalDateTime.class)))
        .thenReturn(Future.succeededFuture(new ResetToken(1, "token123", LocalDateTime.now().plusHours(24))));
    when(redisService.set(anyString(), anyString(), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));
    when(kafkaService.sendMessage(anyString(), anyString(), any())).thenReturn(Future.failedFuture("Kafka down"));

    service.forgotPassword("alice@example.com")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isTrue();
          ctx.completeNow();
        })));
  }

  /* ─── resetPassword ─── */

  @Test
  @DisplayName("resetPassword resets password using cached token")
  void resetPasswordWithCachedToken(VertxTestContext ctx) {
    mockTracing();
    var request = ResetPasswordRequest.builder()
        .resetToken("valid-token")
        .password("new-password")
        .confirmPassword("new-password")
        .build();

    when(redisService.get("resetToken:valid-token")).thenReturn(Future.succeededFuture("1"));
    when(userRepository.updateUserPassword(1, "new-password"))
        .thenReturn(Future.succeededFuture(aUser()));
    when(resetTokenRepository.deleteResetToken(1)).thenReturn(Future.succeededFuture());
    when(redisService.delete("resetToken:valid-token")).thenReturn(Future.succeededFuture(1L));

    service.resetPassword(request)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("resetPassword falls back to DB when cache misses")
  void resetPasswordWithDbToken(VertxTestContext ctx) {
    mockTracing();
    var request = ResetPasswordRequest.builder()
        .resetToken("db-token")
        .password("new-password")
        .confirmPassword("new-password")
        .build();
    var resetToken = new ResetToken(1, "db-token", LocalDateTime.now().plusHours(24));

    when(redisService.get("resetToken:db-token")).thenReturn(Future.succeededFuture(null));
    when(resetTokenRepository.findByToken("db-token")).thenReturn(Future.succeededFuture(resetToken));
    when(userRepository.updateUserPassword(1, "new-password"))
        .thenReturn(Future.succeededFuture(aUser()));
    when(resetTokenRepository.deleteResetToken(1)).thenReturn(Future.succeededFuture());
    when(redisService.delete("resetToken:db-token")).thenReturn(Future.succeededFuture(1L));

    service.resetPassword(request)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("resetPassword fails when passwords do not match")
  void resetPasswordMismatch(VertxTestContext ctx) {
    mockTracing();
    var request = ResetPasswordRequest.builder()
        .resetToken("token")
        .password("password1")
        .confirmPassword("password2")
        .build();

    service.resetPassword(request)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("Passwords do not match");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("resetPassword fails when token is invalid")
  void resetPasswordInvalidToken(VertxTestContext ctx) {
    mockTracing();
    var request = ResetPasswordRequest.builder()
        .resetToken("invalid-token")
        .password("new-password")
        .confirmPassword("new-password")
        .build();

    when(redisService.get("resetToken:invalid-token")).thenReturn(Future.succeededFuture(null));
    when(resetTokenRepository.findByToken("invalid-token")).thenReturn(Future.succeededFuture(null));

    service.resetPassword(request)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("Invalid or expired reset token");
          ctx.completeNow();
        })));
  }

  /* ─── verifyCode ─── */

  @Test
  @DisplayName("verifyCode verifies user and sends success email")
  void verifyCodeSuccess(VertxTestContext ctx) {
    mockTracing();
    var user = aUser();

    when(userRepository.findByVerificationCode("valid-code")).thenReturn(Future.succeededFuture(user));
    when(userRepository.updateUserIsVerified(1, true)).thenReturn(Future.succeededFuture(user));
    when(redisService.delete("verification:alice@example.com")).thenReturn(Future.succeededFuture(1L));
    when(kafkaService.sendMessage(anyString(), anyString(), any())).thenReturn(Future.succeededFuture());

    service.verifyCode("valid-code")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isTrue();
          verify(kafkaService).sendMessage(eq("email-service-topic-auth-verify-code-success"), eq("1"), any());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("verifyCode fails when code is invalid")
  void verifyCodeInvalid(VertxTestContext ctx) {
    mockTracing();

    when(userRepository.findByVerificationCode("bad-code")).thenReturn(Future.succeededFuture(null));

    service.verifyCode("bad-code")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("Invalid verification code");
          ctx.completeNow();
        })));
  }
}
