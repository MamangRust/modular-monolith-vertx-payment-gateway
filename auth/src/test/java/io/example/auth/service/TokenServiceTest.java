package io.example.auth.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class TokenServiceTest {

  private TokenService tokenService;
  private JWTAuth jwtAuth;

  @BeforeEach
  void setUp(Vertx vertx) {
    jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
        .addPubSecKey(new PubSecKeyOptions()
            .setAlgorithm("HS256")
            .setBuffer("test-secret-key-for-jwt-at-least-32-chars!!")));

    tokenService = new TokenService(jwtAuth);
  }

  @Test
  @DisplayName("createAccessToken returns a valid JWT with correct subject")
  void createAccessToken(VertxTestContext ctx) {
    var token = tokenService.createAccessToken(42);

    ctx.verify(() -> {
      assertThat(token).isNotNull().isNotEmpty();

      jwtAuth.authenticate(new JsonObject().put("token", token))
          .onSuccess(user -> {
            ctx.verify(() -> {
              assertThat(user).isNotNull();
              assertThat(user.principal().getString("sub")).isEqualTo("42");
              assertThat(user.principal().getInteger("userId")).isEqualTo(42);
              ctx.completeNow();
            });
          })
          .onFailure(ctx::failNow);
    });
  }

  @Test
  @DisplayName("createRefreshToken returns a valid JWT with longer expiry")
  void createRefreshToken(VertxTestContext ctx) {
    var token = tokenService.createRefreshToken(99);

    ctx.verify(() -> {
      assertThat(token).isNotNull().isNotEmpty();

      jwtAuth.authenticate(new JsonObject().put("token", token))
          .onSuccess(user -> {
            ctx.verify(() -> {
              assertThat(user.principal().getString("sub")).isEqualTo("99");
              ctx.completeNow();
            });
          })
          .onFailure(ctx::failNow);
    });
  }

  @Test
  @DisplayName("access and refresh tokens are different for the same userId")
  void tokensAreDistinct(VertxTestContext ctx) {
    var accessToken = tokenService.createAccessToken(7);
    var refreshToken = tokenService.createRefreshToken(7);

    ctx.verify(() -> {
      assertThat(accessToken).isNotEqualTo(refreshToken);

      jwtAuth.authenticate(new JsonObject().put("token", accessToken))
          .compose(accessUser ->
              jwtAuth.authenticate(new JsonObject().put("token", refreshToken))
                  .map(refreshUser -> {
                    assertThat(accessUser.principal().getString("sub"))
                        .isEqualTo("7");
                    assertThat(refreshUser.principal().getString("sub"))
                        .isEqualTo("7");
                    return true;
                  }))
          .onSuccess(ignored -> ctx.completeNow())
          .onFailure(ctx::failNow);
    });
  }
}
