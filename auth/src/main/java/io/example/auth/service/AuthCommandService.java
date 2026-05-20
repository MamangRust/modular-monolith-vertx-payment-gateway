package io.example.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.auth.model.AuthUser;
import io.example.auth.model.RefreshToken;
import io.example.auth.repository.AuthCommandRepository;
import io.example.auth.repository.AuthQueryRepository;
import io.example.common.service.RedisService;
import io.example.common.observability.TracingMetrics;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import lombok.AllArgsConstructor;
import lombok.Data;

public class AuthCommandService {
  private static final Logger log = LoggerFactory.getLogger(AuthCommandService.class);

  private final AuthCommandRepository repository;
  private final AuthQueryRepository queryRepository;
  private final RedisService redis;
  private final JWTAuth jwt;
  private final TracingMetrics metrics;

  public AuthCommandService(
      AuthCommandRepository repository,
      AuthQueryRepository queryRepository,
      RedisService redis,
      JWTAuth jwt,
      TracingMetrics metrics) {
    this.repository = repository;
    this.queryRepository = queryRepository;
    this.redis = redis;
    this.jwt = jwt;
    this.metrics = metrics;
  }

  @Data
  @AllArgsConstructor
  public static class LoginResult {
    private String accessToken;
    private String refreshToken;
  }

  public Future<AuthUser> registerUser(String firstname, String lastname, String email, String plainPassword, String confirmPassword) {
    var ctx = metrics.startSpan("AuthCommandService.registerUser",
        Attributes.builder().put("auth.email", Objects.requireNonNull(email)).build());

    if (!plainPassword.equals(confirmPassword)) {
      metrics.completeSpanError(ctx, "registerUser", "Password mismatch");
      return Future.failedFuture("Password and confirm password do not match");
    }

    String hashed = BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray());

    return repository.createUser(firstname, lastname, email, hashed)
        .compose(u -> repository.assignDefaultAdminRole(u.getUserId())
            .compose(v -> queryRepository.getUserByIdWithRoles(u.getUserId())))
        .onSuccess(u -> metrics.completeSpanSuccess(ctx, "registerUser", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "registerUser", Objects.requireNonNullElse(e.getMessage(), "Unknown error")));
  }

  public Future<LoginResult> loginUser(String email, String password) {
    var ctx = metrics.startSpan("AuthCommandService.loginUser",
        Attributes.builder().put("auth.email", Objects.requireNonNull(email)).build());

    return queryRepository.getUserByEmailWithRoles(email)
        .compose(user -> {
          if (user == null) return Future.failedFuture("Invalid credentials");

          BCrypt.Result res = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());
          if (!res.verified) return Future.failedFuture("Invalid credentials");

          String accessToken = generateAccessToken(user);
          String jti = UUID.randomUUID().toString();
          String refreshToken = generateRefreshToken(user.getUserId(), jti);
          LocalDateTime expiry = LocalDateTime.now().plusDays(7);

          return repository.deleteRefreshTokenByUserId(user.getUserId())
              .recover(err -> {
                log.warn("Ignored error wiping old tokens: {}", err.getMessage());
                return Future.succeededFuture();
              })
              .compose(v -> repository.createRefreshToken(user.getUserId(), refreshToken, expiry))
              .compose(rt -> {
                String key = "session:" + user.getUserId();
                JsonObject session = new JsonObject()
                    .put("userId", user.getUserId())
                    .put("email", user.getEmail())
                    .put("accessToken", accessToken)
                    .put("refreshToken", rt.getToken())
                    .put("roles", user.getRoles());
                return redis.setJson(key, session, Duration.ofHours(1)).map(rt);
              })
              .map(rt -> new LoginResult(accessToken, rt.getToken()));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "loginUser", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "loginUser", Objects.requireNonNullElse(e.getMessage(), "Unknown error")));
  }

  public Future<LoginResult> rotateToken(String inputRefreshToken) {
    var ctx = metrics.startSpan("AuthCommandService.rotateToken");

    return queryRepository.findRefreshTokenByToken(inputRefreshToken)
        .compose(rt -> {
          if (rt == null) return Future.failedFuture("Invalid refresh token");
          
          LocalDateTime now = LocalDateTime.now();
          if (rt.getExpiration().isBefore(now)) {
            return Future.failedFuture("Refresh token expired");
          }

          boolean needsRenewal = rt.getExpiration().minusDays(1).isBefore(now);

          return queryRepository.getUserByIdWithRoles(rt.getUserId())
              .compose(user -> {
                if (user == null) return Future.failedFuture("User associated with token not found");

                String accessToken = generateAccessToken(user);
                Future<String> tokenResolution;

                if (needsRenewal) {
                  String newJti = UUID.randomUUID().toString();
                  String newRtStr = generateRefreshToken(user.getUserId(), newJti);
                  LocalDateTime newExpiry = LocalDateTime.now().plusDays(7);
                  tokenResolution = repository.deleteRefreshTokenByUserId(user.getUserId())
                      .compose(v -> repository.createRefreshToken(user.getUserId(), newRtStr, newExpiry))
                      .map(RefreshToken::getToken);
                } else {
                  tokenResolution = Future.succeededFuture(inputRefreshToken);
                }

                return tokenResolution.compose(finalRt -> {
                  String key = "session:" + user.getUserId();
                  JsonObject session = new JsonObject()
                      .put("userId", user.getUserId())
                      .put("email", user.getEmail())
                      .put("accessToken", accessToken)
                      .put("refreshToken", finalRt)
                      .put("roles", user.getRoles());
                  return redis.setJson(key, session, Duration.ofHours(1)).map(finalRt);
                }).map(finalRt -> new LoginResult(accessToken, finalRt));
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "rotateToken", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "rotateToken", Objects.requireNonNullElse(e.getMessage(), "Unknown error")));
  }

  private String generateAccessToken(AuthUser user) {
    return jwt.generateToken(
        new JsonObject()
            .put("sub", "access")
            .put("userId", user.getUserId())
            .put("email", user.getEmail())
            .put("roleNames", user.getRoles()),
        new JWTOptions().setExpiresInMinutes(60));
  }

  private String generateRefreshToken(Integer userId, String jti) {
    return jwt.generateToken(
        new JsonObject()
            .put("sub", "refresh")
            .put("userId", userId)
            .put("jti", jti),
        new JWTOptions().setExpiresInMinutes(60 * 24 * 7));
  }
}
