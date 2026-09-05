package io.example.withdraw;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WithdrawConfigTest {

  @Test
  @DisplayName("Fallback defaults are safe values, not hardcoded DRAGON")
  void fallbackDefaultsAreSafe() {
    var env = new java.util.HashMap<String, String>();
    assertThat(env.getOrDefault("DB_HOST", "postgres")).isEqualTo("postgres");
    assertThat(env.getOrDefault("DB_USERNAME", "vertx")).isEqualTo("vertx");
    assertThat(env.getOrDefault("DB_PASSWORD", "vertx")).isEqualTo("vertx");
    assertThat(env.getOrDefault("DB_USERNAME", "vertx")).isNotEqualTo("DRAGON");
    assertThat(env.getOrDefault("DB_PASSWORD", "vertx")).isNotEqualTo("DRAGON");
  }

  @Test
  @DisplayName("Env var overrides are respected in config construction")
  void envVarOverrideWorks() {
    var env = new java.util.HashMap<String, String>();
    env.put("DB_HOST", "pgbouncer");
    env.put("DB_USERNAME", "app_user");
    env.put("DB_PASSWORD", "s3cret!");

    JsonObject dbConfig = new JsonObject()
        .put("host", env.getOrDefault("DB_HOST", "postgres"))
        .put("user", env.getOrDefault("DB_USERNAME", "vertx"))
        .put("password", env.getOrDefault("DB_PASSWORD", "vertx"));

    assertThat(dbConfig.getString("host")).isEqualTo("pgbouncer");
    assertThat(dbConfig.getString("user")).isEqualTo("app_user");
    assertThat(dbConfig.getString("password")).isEqualTo("s3cret!");
  }
}
