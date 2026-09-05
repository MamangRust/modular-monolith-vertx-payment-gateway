package io.example.common.chaos;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class ChaosManagerTest {

  @Test
  @DisplayName("chaos is disabled when CHAOS_ENABLED is absent")
  void disabledByDefault() {
    assertThat(ChaosManager.isChaosEnabled(Map.of())).isFalse();
  }

  @Test
  @DisplayName("chaos is enabled only for CHAOS_ENABLED=true")
  void enabledForTrue() {
    assertThat(ChaosManager.isChaosEnabled(Map.of("CHAOS_ENABLED", "true"))).isTrue();
    assertThat(ChaosManager.isChaosEnabled(Map.of("CHAOS_ENABLED", "TRUE"))).isTrue();
    assertThat(ChaosManager.isChaosEnabled(Map.of("CHAOS_ENABLED", " true "))).isTrue();
  }

  @Test
  @DisplayName("chaos stays disabled for false, 1, yes and garbage values")
  void disabledForNonTrue() {
    assertThat(ChaosManager.isChaosEnabled(Map.of("CHAOS_ENABLED", "false"))).isFalse();
    assertThat(ChaosManager.isChaosEnabled(Map.of("CHAOS_ENABLED", "1"))).isFalse();
    assertThat(ChaosManager.isChaosEnabled(Map.of("CHAOS_ENABLED", "yes"))).isFalse();
    assertThat(ChaosManager.isChaosEnabled(Map.of("CHAOS_ENABLED", ""))).isFalse();
  }

  @Test
  @DisplayName("no policies are loaded while chaos is disabled")
  void skipsConfigLoadWhenDisabled() {
    ChaosManager manager = new ChaosManager("chaos.yaml");
    assertThat(manager.getPolicies()).isEmpty();
    assertThat(manager.evaluate("sql", "saldos")).isNull();
    assertThat(manager.isEnabled()).isFalse();
  }

  @Test
  @DisplayName("enabled manager loads and matches exact, wildcard and regex targets")
  void evaluatesConfiguredPolicies() throws Exception {
    Path config = Files.createTempFile("chaos-test-", ".yaml");
    Files.writeString(config, """
        policies:
          - name: http-fault
            type: http
            target: GET:/api/payments/*
            enabled: true
            errorChance: 1.0
            errorCode: 503
          - name: sql-fault
            type: sql
            target: users
            enabled: true
            errorChance: 1.0
            errorMessage: database-down
        """);
    try {
      ChaosManager manager = new ChaosManager(config.toString(), true);
      assertThat(manager.isEnabled()).isTrue();
      assertThat(manager.evaluate("http", "GET:/api/payments/42").getName()).isEqualTo("http-fault");
      assertThat(manager.evaluate("sql", "users").getName()).isEqualTo("sql-fault");
    } finally {
      Files.deleteIfExists(config);
    }
  }

  @Test
  @DisplayName("HTTP chaos middleware deterministically returns configured fault")
  void injectsHttpFault(Vertx vertx, VertxTestContext ctx) throws Exception {
    Path config = Files.createTempFile("chaos-http-", ".yaml");
    Files.writeString(config, """
        policies:
          - name: forced-http-fault
            type: http
            target: GET:/fault
            enabled: true
            errorChance: 1.0
            errorCode: 429
            errorBody: '{"error":"rate_limited"}'
        """);

    ChaosManager manager = new ChaosManager(config.toString(), true);
    Router router = Router.router(vertx);
    router.route().handler(new ChaosHttpMiddleware(manager));
    router.get("/fault").handler(request -> request.response().setStatusCode(200).end());

    vertx.createHttpServer().requestHandler(router).listen(0).onComplete(ctx.succeeding(server -> {
      int port = server.actualPort();
      vertx.createHttpClient().request(HttpMethod.GET, port, "localhost", "/fault")
          .compose(request -> request.send())
          .onComplete(ctx.succeeding(response -> ctx.verify(() -> {
            assertThat(response.statusCode()).isEqualTo(429);
            assertThat(response.getHeader("Content-Type")).contains("application/json");
            response.body().onComplete(ctx.succeeding(body -> ctx.verify(() -> {
              assertThat(body.toString()).contains("rate_limited");
              ctx.completeNow();
              server.close();
              Files.deleteIfExists(config);
            })));
          })));
    }));
  }
}
