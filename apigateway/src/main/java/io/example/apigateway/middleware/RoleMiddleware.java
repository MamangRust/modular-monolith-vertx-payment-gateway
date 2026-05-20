package io.example.apigateway.middleware;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

public final class RoleMiddleware {
  private RoleMiddleware() {}

  public static Handler<RoutingContext> requireRole(String role) {
    return ctx -> {
      if (ctx.user() == null || ctx.user().principal() == null) {
        ctx.response().setStatusCode(401).end("Unauthorized");
        return;
      }

      JsonArray roles = ctx.user().principal().getJsonArray("roleNames");

      if (roles == null || !roles.contains(role)) {
        ctx.response().setStatusCode(403).end("Forbidden");
        return;
      }

      ctx.next();
    };
  }
}
