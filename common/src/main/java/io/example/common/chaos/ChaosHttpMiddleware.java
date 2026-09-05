package io.example.common.chaos;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaosHttpMiddleware implements Handler<RoutingContext> {
  private static final Logger log = LoggerFactory.getLogger(ChaosHttpMiddleware.class);

  private final ChaosManager manager;

  /** Snapshot of {@code CHAOS_ENABLED} at construction; when false every request passes through. */
  private final boolean enabled;

  public ChaosHttpMiddleware(ChaosManager manager) {
    this(manager, manager.isEnabled());
  }

  /** Testable constructor for deterministic middleware integration tests. */
  ChaosHttpMiddleware(ChaosManager manager, boolean enabled) {
    this.manager = manager;
    this.enabled = enabled;
    if (enabled) {
      log.warn("⚠️ {}=true — HTTP chaos middleware installed",
          ChaosManager.CHAOS_ENABLED_ENV);
    }
  }

  @Override
  public void handle(RoutingContext ctx) {
    if (!enabled) {
      ctx.next();
      return;
    }

    String path = ctx.request().path();
    String method = ctx.request().method().name();
    String fullTarget = method + ":" + path;

    // Check for ignored paths (like health check or chaos dashboard itself)
    if (path.equals("/health") || path.startsWith("/api/chaos")) {
      ctx.next();
      return;
    }

    ChaosPolicy temp = manager.evaluate("http", fullTarget);
    if (temp == null) {
      temp = manager.evaluate("http", path);
    }
    final ChaosPolicy policy = temp;

    if (policy != null && policy.isEnabled() && Math.random() < policy.getErrorChance()) {
      log.info("🔥 Injecting HTTP chaos [Policy: {}] to request: {} {}", policy.getName(), method, path);

      if (policy.isDropConnection()) {
        log.warn("🔌 Closing connection abruptly (drop_connection) for: {} {}", method, path);
        ctx.request().connection().close();
        return;
      }

      long latency = policy.getLatencyMs();
      if (latency > 0) {
        ctx.vertx().setTimer(latency, timerId -> executeHttpFault(ctx, policy));
      } else {
        executeHttpFault(ctx, policy);
      }
    } else {
      ctx.next();
    }
  }

  private void executeHttpFault(RoutingContext ctx, ChaosPolicy policy) {
    int code = policy.getErrorCode() != 0 ? policy.getErrorCode() : 503;
    String body = policy.getErrorBody() != null ? policy.getErrorBody() : "{\"error\":\"chaos_fault\",\"message\":\"Simulated chaos error\"}";
    
    ctx.response()
        .setStatusCode(code)
        .putHeader("Content-Type", "application/json")
        .end(body);
  }
}
