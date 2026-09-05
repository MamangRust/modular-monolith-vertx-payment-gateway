package io.example.auth.handler;

import io.example.auth.model.AuthUser;
import io.example.auth.model.TokenResponse;
import io.example.auth.service.IdentityService;
import io.example.auth.service.LoginService;
import io.example.auth.service.PasswordResetService;
import io.example.auth.service.RegisterService;
import io.example.common.exception.grpc.BadRequestException;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.Auth.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class AuthHandlerTest {

  @Mock
  private RegisterService registerService;

  @Mock
  private IdentityService identityService;

  @Mock
  private PasswordResetService passwordResetService;

  @Mock
  private LoginService loginService;

  private AuthHandler handler;

  @BeforeEach
  void setUp() {
    handler = new AuthHandler(registerService, identityService, passwordResetService, loginService);
  }

  private static AuthUser aUser() {
    var now = LocalDateTime.parse("2026-06-26T10:00:00");
    return AuthUser.builder()
        .userId(42)
        .firstname("Alice")
        .lastname("Wonderland")
        .email("alice@example.com")
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /* ─── registerUser ─── */

  @Test
  @DisplayName("registerUser delegates and returns success response")
  void registerUser(VertxTestContext ctx) {
    var user = aUser();
    when(registerService.register(any())).thenReturn(Future.succeededFuture(user));

    var req = pb.Auth.RegisterRequest.newBuilder()
        .setFirstname("Alice")
        .setLastname("Wonderland")
        .setEmail("alice@example.com")
        .setPassword("password123")
        .build();

    handler.registerUser(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).contains("registered");
          assertThat(resp.getData()).isNotNull();
          assertThat(resp.getData().getId()).isEqualTo(42);
          assertThat(resp.getData().getEmail()).isEqualTo("alice@example.com");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("registerUser delegates error when service fails")
  void registerUserError(VertxTestContext ctx) {
    when(registerService.register(any()))
        .thenReturn(Future.failedFuture(new BadRequestException("User with this email already exists")));

    var req = pb.Auth.RegisterRequest.newBuilder()
        .setFirstname("Alice").setLastname("Wonderland")
        .setEmail("existing@example.com").setPassword("pass")
        .build();

    handler.registerUser(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }

  /* ─── loginUser ─── */

  @Test
  @DisplayName("loginUser delegates and returns token response")
  void loginUser(VertxTestContext ctx) {
    var tokens = TokenResponse.builder()
        .accessToken("access-token")
        .refreshToken("refresh-token")
        .build();
    when(loginService.login(any())).thenReturn(Future.succeededFuture(tokens));

    var req = LoginRequest.newBuilder()
        .setEmail("alice@example.com")
        .setPassword("password123")
        .build();

    handler.loginUser(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getAccessToken()).isEqualTo("access-token");
          assertThat(resp.getData().getRefreshToken()).isEqualTo("refresh-token");
          ctx.completeNow();
        })));
  }

  /* ─── refreshToken ─── */

  @Test
  @DisplayName("refreshToken delegates and returns new tokens")
  void refreshToken(VertxTestContext ctx) {
    var tokens = TokenResponse.builder()
        .accessToken("new-access")
        .refreshToken("new-refresh")
        .build();
    when(identityService.refreshToken("old-token")).thenReturn(Future.succeededFuture(tokens));

    var req = RefreshTokenRequest.newBuilder()
        .setRefreshToken("old-token")
        .build();

    handler.refreshToken(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getAccessToken()).isEqualTo("new-access");
          assertThat(resp.getData().getRefreshToken()).isEqualTo("new-refresh");
          ctx.completeNow();
        })));
  }

  /* ─── getMe ─── */

  @Test
  @DisplayName("getMe delegates and returns user response")
  void getMe(VertxTestContext ctx) {
    var user = aUser();
    when(identityService.getMe(42)).thenReturn(Future.succeededFuture(user));

    var req = GetMeRequest.newBuilder()
        .setUserId(42)
        .build();

    handler.getMe(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(42);
          assertThat(resp.getData().getEmail()).isEqualTo("alice@example.com");
          ctx.completeNow();
        })));
  }

  /* ─── verifyCode ─── */

  @Test
  @DisplayName("verifyCode delegates and returns success")
  void verifyCode(VertxTestContext ctx) {
    when(passwordResetService.verifyCode("abc123")).thenReturn(Future.succeededFuture(true));

    var req = VerifyCodeRequest.newBuilder()
        .setCode("abc123")
        .build();

    handler.verifyCode(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).contains("verified");
          ctx.completeNow();
        })));
  }

  /* ─── forgotPassword ─── */

  @Test
  @DisplayName("forgotPassword delegates and returns success")
  void forgotPassword(VertxTestContext ctx) {
    when(passwordResetService.forgotPassword("alice@example.com"))
        .thenReturn(Future.succeededFuture(true));

    var req = ForgotPasswordRequest.newBuilder()
        .setEmail("alice@example.com")
        .build();

    handler.forgotPassword(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).contains("alice@example.com");
          ctx.completeNow();
        })));
  }

  /* ─── resetPassword ─── */

  @Test
  @DisplayName("resetPassword delegates and returns success")
  void resetPassword(VertxTestContext ctx) {
    when(passwordResetService.resetPassword(any())).thenReturn(Future.succeededFuture(true));

    var req = pb.Auth.ResetPasswordRequest.newBuilder()
        .setResetToken("reset-token")
        .setPassword("new-password")
        .setConfirmPassword("new-password")
        .build();

    handler.resetPassword(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).contains("reset");
          ctx.completeNow();
        })));
  }
}
