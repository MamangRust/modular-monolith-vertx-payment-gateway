package io.example.apigateway.middleware;

import io.vertx.core.Handler;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtMiddlewareTest {

  @Mock
  private JWTAuth jwtAuth;

  @Test
  @DisplayName("returns JWTAuthHandler instance")
  void returnsHandler() {
    Handler<RoutingContext> handler = JwtMiddleware.jwt(jwtAuth);
    assertThat(handler).isNotNull();
  }
}
