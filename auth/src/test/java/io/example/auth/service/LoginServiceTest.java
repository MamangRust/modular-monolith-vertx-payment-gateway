package io.example.auth.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.example.auth.domain.requests.AuthRequest;
import io.example.auth.model.AuthUser;
import io.example.auth.repository.UserRepository;
import io.example.common.exception.grpc.ForbiddenException;
import io.example.common.exception.grpc.UnauthorizedException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
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
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class LoginServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private RedisService redisService;

  @Mock
  private TokenService tokenService;

  @Mock
  private TracingMetrics tracingMetrics;

  private LoginService service;

  @BeforeEach
  void setUp() {
    service = new LoginService(userRepository, redisService, tokenService, tracingMetrics);
  }

  private void mockTracing() {
    when(tracingMetrics.startSpan(anyString())).thenReturn(new TracingContext(null, null));
  }

  private AuthUser aVerifiedUser() {
    return AuthUser.builder()
        .userId(1)
        .firstname("Alice")
        .lastname("Wonderland")
        .email("alice@example.com")
        // BCrypt hash of "correct-password"
        .password("$2a$12$LJ3m4ys3Lg3YOCw5qgWAh.cDqV3r8GHhHVgGFH1BHQ3s5GxvZ3qOq")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private AuthRequest aLoginRequest() {
    return AuthRequest.builder()
        .email("alice@example.com")
        .password("correct-password")
        .build();
  }

  /* ─── login success ─── */

  @Test
  @DisplayName("login succeeds with valid credentials")
  void loginSuccess(VertxTestContext ctx) {
    mockTracing();
    var request = aLoginRequest();
    var user = aVerifiedUser();

    // Generate a real BCrypt hash that verifies against "correct-password"
    String realHash = BCrypt.withDefaults().hashToString(12, "correct-password".toCharArray());
    user.setPassword(realHash);

    when(redisService.exists("account_locked:alice@example.com")).thenReturn(Future.succeededFuture(false));
    when(userRepository.findByEmailAndVerify(request.getEmail())).thenReturn(Future.succeededFuture(user));
    when(tokenService.createAccessToken(1)).thenReturn("access-token");
    when(tokenService.createRefreshToken(1)).thenReturn("refresh-token");
    when(redisService.delete("failed_login:alice@example.com")).thenReturn(Future.succeededFuture(1L));
    when(redisService.delete("account_locked:alice@example.com")).thenReturn(Future.succeededFuture(1L));

    service.login(request)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getAccessToken()).isEqualTo("access-token");
          assertThat(resp.getRefreshToken()).isEqualTo("refresh-token");
          ctx.completeNow();
        })));
  }

  /* ─── account locked ─── */

  @Test
  @DisplayName("login fails when account is locked")
  void loginAccountLocked(VertxTestContext ctx) {
    mockTracing();

    when(redisService.exists("account_locked:alice@example.com")).thenReturn(Future.succeededFuture(true));

    var request = aLoginRequest();
    service.login(request)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(ForbiddenException.class)
              .hasMessage("Account is locked due to too many failed attempts");
          ctx.completeNow();
        })));
  }

  /* ─── user not found ─── */

  @Test
  @DisplayName("login fails when user not found or not verified")
  void loginUserNotFound(VertxTestContext ctx) {
    mockTracing();

    when(redisService.exists("account_locked:alice@example.com")).thenReturn(Future.succeededFuture(false));
    when(userRepository.findByEmailAndVerify("alice@example.com")).thenReturn(Future.succeededFuture(null));

    service.login(aLoginRequest())
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(UnauthorizedException.class)
              .hasMessage("Invalid credentials");
          ctx.completeNow();
        })));
  }

  /* ─── wrong password ─── */

  @Test
  @DisplayName("login fails with wrong password (first attempt)")
  void loginWrongPassword(VertxTestContext ctx) {
    mockTracing();
    var user = aVerifiedUser();
    // Hash a different password than what the request will provide
    String realHash = BCrypt.withDefaults().hashToString(12, "actual-password".toCharArray());
    user.setPassword(realHash);

    when(redisService.exists("account_locked:alice@example.com")).thenReturn(Future.succeededFuture(false));
    when(userRepository.findByEmailAndVerify("alice@example.com")).thenReturn(Future.succeededFuture(user));
    when(redisService.incr("failed_login:alice@example.com")).thenReturn(Future.succeededFuture(1L));
    when(redisService.expire(anyString(), any(Duration.class))).thenReturn(Future.succeededFuture());

    var request = AuthRequest.builder()
        .email("alice@example.com")
        .password("wrong-password")
        .build();

    service.login(request)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(UnauthorizedException.class)
              .hasMessage("Invalid credentials");
          ctx.completeNow();
        })));
  }

  /* ─── account lockout after 5 attempts ─── */

  @Test
  @DisplayName("login locks account after 5 failed attempts")
  void loginAccountLockout(VertxTestContext ctx) {
    mockTracing();
    var user = aVerifiedUser();
    String realHash = BCrypt.withDefaults().hashToString(12, "actual-password".toCharArray());
    user.setPassword(realHash);

    when(redisService.exists("account_locked:alice@example.com")).thenReturn(Future.succeededFuture(false));
    when(userRepository.findByEmailAndVerify("alice@example.com")).thenReturn(Future.succeededFuture(user));
    when(redisService.incr("failed_login:alice@example.com")).thenReturn(Future.succeededFuture(5L));
    when(redisService.set(anyString(), anyString(), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var request = AuthRequest.builder()
        .email("alice@example.com")
        .password("wrong-password")
        .build();

    service.login(request)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(ForbiddenException.class)
              .hasMessage("Account locked due to 5 failed attempts");
          ctx.completeNow();
        })));
  }
}
