package io.example.apigateway.middleware;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.http.HttpServerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleMiddlewareTest {

  @Mock
  private RoutingContext ctx;

  @Mock
  private User user;

  @Mock
  private HttpServerResponse response;

  @Test
  @DisplayName("returns 401 when no user is authenticated")
  void noUserReturns401() {
    when(ctx.user()).thenReturn(null);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(401)).thenReturn(response);

    RoleMiddleware.requireRole("ROLE_ADMIN").handle(ctx);

    verify(response).setStatusCode(401);
    verify(response).end("Unauthorized");
  }

  @Test
  @DisplayName("returns 401 when user principal is null")
  void nullPrincipalReturns401() {
    when(ctx.user()).thenReturn(user);
    when(user.principal()).thenReturn(null);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(401)).thenReturn(response);

    RoleMiddleware.requireRole("ROLE_ADMIN").handle(ctx);

    verify(response).setStatusCode(401);
    verify(response).end("Unauthorized");
  }

  @Test
  @DisplayName("returns 403 when roleNames is null")
  void nullRoleNamesReturns403() {
    var principal = new JsonObject();
    when(ctx.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(403)).thenReturn(response);

    RoleMiddleware.requireRole("ROLE_ADMIN").handle(ctx);

    verify(response).setStatusCode(403);
    verify(response).end("Forbidden");
  }

  @Test
  @DisplayName("returns 403 when required role is not present")
  void missingRoleReturns403() {
    var principal = new JsonObject().put("roleNames", new JsonArray().add("ROLE_USER"));
    when(ctx.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(403)).thenReturn(response);

    RoleMiddleware.requireRole("ROLE_ADMIN").handle(ctx);

    verify(response).setStatusCode(403);
    verify(response).end("Forbidden");
  }

  @Test
  @DisplayName("calls ctx.next() when required role is present")
  void correctRoleProceeds() {
    var principal = new JsonObject().put("roleNames",
        new JsonArray().add("ROLE_ADMIN").add("ROLE_USER"));
    when(ctx.user()).thenReturn(user);
    when(user.principal()).thenReturn(principal);

    RoleMiddleware.requireRole("ROLE_ADMIN").handle(ctx);

    verify(ctx).next();
    verifyNoInteractions(response);
  }
}
