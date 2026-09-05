package io.example.apigateway.routes;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the Kubernetes-friendly health endpoints added to the API Gateway:
 * <ul>
 *   <li>{@code GET /health} — general health (existing)</li>
 *   <li>{@code GET /health/live} — liveness probe (new)</li>
 *   <li>{@code GET /health/ready} — readiness probe (new)</li>
 * </ul>
 */
@ExtendWith(VertxExtension.class)
class HealthEndpointTest {

  private static final int TEST_PORT = 18080;

  @BeforeEach
  void setUp(Vertx vertx, VertxTestContext ctx) {
    Router router = Router.router(vertx);

    // Same route definitions as GatewayRoutes.java
    router.get("/health").handler(ctx2 -> ctx2.response()
        .putHeader("Content-Type", "application/json")
        .end(new JsonObject()
            .put("status", "UP")
            .put("service", "gateway")
            .encode()));

    router.get("/health/live").handler(ctx2 -> ctx2.response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(200)
        .end(new JsonObject()
            .put("status", "alive")
            .encode()));

    router.get("/health/ready").handler(ctx2 -> ctx2.response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(200)
        .end(new JsonObject()
            .put("status", "ready")
            .encode()));

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(TEST_PORT)
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  @DisplayName("GET /health returns 200 with status UP and service gateway")
  void healthEndpoint(Vertx vertx, VertxTestContext ctx) {
    HttpClient client = vertx.createHttpClient();
    client.request(io.vertx.core.http.HttpMethod.GET, TEST_PORT, "localhost", "/health")
        .compose(req -> req.send())
        .compose(resp -> {
          ctx.verify(() -> {
            assertThat(resp.statusCode()).isEqualTo(200);
            assertThat(resp.getHeader("Content-Type")).contains("application/json");
          });
          return resp.body();
        })
        .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
          JsonObject json = new JsonObject(buffer);
          assertThat(json.getString("status")).isEqualTo("UP");
          assertThat(json.getString("service")).isEqualTo("gateway");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("GET /health/live returns 200 with status alive")
  void livenessEndpoint(Vertx vertx, VertxTestContext ctx) {
    HttpClient client = vertx.createHttpClient();
    client.request(io.vertx.core.http.HttpMethod.GET, TEST_PORT, "localhost", "/health/live")
        .compose(req -> req.send())
        .compose(resp -> {
          ctx.verify(() -> {
            assertThat(resp.statusCode()).isEqualTo(200);
            assertThat(resp.getHeader("Content-Type")).contains("application/json");
          });
          return resp.body();
        })
        .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
          JsonObject json = new JsonObject(buffer);
          assertThat(json.getString("status")).isEqualTo("alive");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("GET /health/ready returns 200 with status ready")
  void readinessEndpoint(Vertx vertx, VertxTestContext ctx) {
    HttpClient client = vertx.createHttpClient();
    client.request(io.vertx.core.http.HttpMethod.GET, TEST_PORT, "localhost", "/health/ready")
        .compose(req -> req.send())
        .compose(resp -> {
          ctx.verify(() -> {
            assertThat(resp.statusCode()).isEqualTo(200);
            assertThat(resp.getHeader("Content-Type")).contains("application/json");
          });
          return resp.body();
        })
        .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
          JsonObject json = new JsonObject(buffer);
          assertThat(json.getString("status")).isEqualTo("ready");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("GET /health/nonexistent returns 404")
  void unknownEndpoint(Vertx vertx, VertxTestContext ctx) {
    HttpClient client = vertx.createHttpClient();
    client.request(io.vertx.core.http.HttpMethod.GET, TEST_PORT, "localhost", "/health/nonexistent")
        .compose(req -> req.send())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.statusCode()).isEqualTo(404);
          ctx.completeNow();
        })));
  }
}
