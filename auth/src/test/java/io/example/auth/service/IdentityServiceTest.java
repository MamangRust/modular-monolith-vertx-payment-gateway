package io.example.auth.service;

import io.example.auth.model.AuthUser;
import io.example.auth.model.TokenResponse;
import io.example.auth.repository.RefreshTokenRepository;
import io.example.auth.repository.UserRepository;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.exception.grpc.UnauthorizedException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class IdentityServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private RedisService redisService;

  @Mock
  private TokenService tokenService;

  @Mock
  private JWTAuth jwtProvider;

  @Mock
  private TracingMetrics tracingMetrics;

  private IdentityService service;

  @BeforeEach
  void setUp() {
    service = new IdentityService(userRepository, refreshTokenRepository, redisService,
        tokenService, jwtProvider, tracingMetrics);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), java.time.Instant.now());
    lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(ctx);
    lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(ctx);
  }

  /* ─── refreshToken ─── */

  @Test
  @DisplayName("refreshToken uses cache hit to rotate token")
  void refreshTokenCacheHit(VertxTestContext ctx) {
    mockTracing();

    when(redisService.get("refreshToken:old-token")).thenReturn(Future.succeededFuture("1"));
    when(redisService.delete("refreshToken:old-token")).thenReturn(Future.succeededFuture(1L));
    when(tokenService.createAccessToken(1)).thenReturn("new-access");
    when(tokenService.createRefreshToken(1)).thenReturn("new-refresh");
    when(redisService.set("refreshToken:new-refresh", "1", Duration.ofHours(24)))
        .thenReturn(Future.succeededFuture("OK"));

    service.refreshToken("old-token")
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getAccessToken()).isEqualTo("new-access");
          assertThat(resp.getRefreshToken()).isEqualTo("new-refresh");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("refreshToken falls back to JWT when cache misses")
  void refreshTokenCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var jwtUser = org.mockito.Mockito.mock(User.class);
    var principal = new JsonObject().put("sub", "1");

    when(redisService.get("refreshToken:old-token")).thenReturn(Future.succeededFuture(null));
    when(jwtProvider.authenticate(any(TokenCredentials.class))).thenReturn(Future.succeededFuture(jwtUser));
    when(jwtUser.principal()).thenReturn(principal);
    when(refreshTokenRepository.deleteRefreshToken("old-token")).thenReturn(Future.succeededFuture());
    when(tokenService.createAccessToken(1)).thenReturn("new-access");
    when(tokenService.createRefreshToken(1)).thenReturn("new-refresh");
    when(refreshTokenRepository.updateRefreshToken(any(), anyString(), any(LocalDateTime.class)))
        .thenReturn(Future.succeededFuture(null));
    when(redisService.set("refreshToken:new-refresh", "1", Duration.ofHours(24)))
        .thenReturn(Future.succeededFuture("OK"));

    service.refreshToken("old-token")
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getAccessToken()).isEqualTo("new-access");
          assertThat(resp.getRefreshToken()).isEqualTo("new-refresh");
          verify(jwtProvider).authenticate(any(TokenCredentials.class));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("refreshToken fails when JWT authentication fails")
  void refreshTokenInvalidJwt(VertxTestContext ctx) {
    mockTracing();

    when(redisService.get("refreshToken:invalid-token")).thenReturn(Future.succeededFuture(null));
    when(jwtProvider.authenticate(any(TokenCredentials.class)))
        .thenReturn(Future.failedFuture("JWT expired"));

    service.refreshToken("invalid-token")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(UnauthorizedException.class)
              .hasMessage("Invalid or expired refresh token");
          ctx.completeNow();
        })));
  }

  /* ─── getMe ─── */

  @Test
  @DisplayName("getMe returns cached user when available")
  void getMeCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cachedUser = AuthUser.builder()
        .userId(1)
        .firstname("Alice")
        .email("alice@example.com")
        .build();

    when(redisService.getJson("user:1", AuthUser.class)).thenReturn(Future.succeededFuture(cachedUser));

    service.getMe(1)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          assertThat(user.getEmail()).isEqualTo("alice@example.com");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMe fetches from DB when cache misses and caches result")
  void getMeCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var dbUser = AuthUser.builder()
        .userId(1)
        .firstname("Alice")
        .email("alice@example.com")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    when(redisService.getJson("user:1", AuthUser.class)).thenReturn(Future.succeededFuture(null));
    when(userRepository.findById(1)).thenReturn(Future.succeededFuture(dbUser));
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getMe(1)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          verify(redisService).setJson("user:1", dbUser, Duration.ofMinutes(5));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMe fails when user not found in DB")
  void getMeUserNotFound(VertxTestContext ctx) {
    mockTracing();

    when(redisService.getJson("user:99", AuthUser.class)).thenReturn(Future.succeededFuture(null));
    when(userRepository.findById(99)).thenReturn(Future.succeededFuture(null));

    service.getMe(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("User not found");
          ctx.completeNow();
        })));
  }
}
