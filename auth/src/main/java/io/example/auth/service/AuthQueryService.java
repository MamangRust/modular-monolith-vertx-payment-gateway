package io.example.auth.service;

import java.util.Objects;

import io.example.auth.model.AuthUser;
import io.example.auth.model.RefreshToken;
import io.example.auth.repository.AuthQueryRepository;
import io.example.common.observability.TracingMetrics;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

public class AuthQueryService {
  private final AuthQueryRepository repository;
  private final TracingMetrics metrics;

  public AuthQueryService(AuthQueryRepository repository, TracingMetrics metrics) {
    this.repository = repository;
    this.metrics = metrics;
  }

  public Future<AuthUser> getUserByEmailWithRoles(String email) {
    var ctx = metrics.startSpan("AuthQueryService.getUserByEmailWithRoles",
        Attributes.builder().put("auth.email", Objects.requireNonNull(email)).build());
    return repository.getUserByEmailWithRoles(email)
        .onSuccess(u -> metrics.completeSpanSuccess(ctx, "getUserByEmailWithRoles", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getUserByEmailWithRoles", e.getMessage()));
  }

  public Future<AuthUser> getUserByIdWithRoles(Integer userId) {
    var ctx = metrics.startSpan("AuthQueryService.getUserByIdWithRoles",
        Attributes.builder().put("user.id", userId).build());
    return repository.getUserByIdWithRoles(userId)
        .onSuccess(u -> metrics.completeSpanSuccess(ctx, "getUserByIdWithRoles", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getUserByIdWithRoles", e.getMessage()));
  }

  public Future<RefreshToken> findRefreshTokenByToken(String token) {
    var ctx = metrics.startSpan("AuthQueryService.findRefreshTokenByToken");
    return repository.findRefreshTokenByToken(token)
        .onSuccess(t -> metrics.completeSpanSuccess(ctx, "findRefreshTokenByToken", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "findRefreshTokenByToken", e.getMessage()));
  }
}
