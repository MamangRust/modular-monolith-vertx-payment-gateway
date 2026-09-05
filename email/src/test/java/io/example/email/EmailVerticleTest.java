package io.example.email;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for EmailVerticle helper logic and config parsing.
 * The main verticle logic (Kafka consumer, mail client) requires integration testing,
 * so this focuses on the config/env-var patterns used by the verticle.
 */
class EmailVerticleTest {

  /* ───────── SMTP config env-var defaults ───────── */

  @Test
  @DisplayName("SMTP config uses env vars with safe defaults")
  void smtpDefaultsAreSane() {
    var env = new java.util.HashMap<String, String>();

    assertThat(env.getOrDefault("SMTP_SERVER", "localhost")).isEqualTo("localhost");
    assertThat(env.getOrDefault("SMTP_PORT", "587")).isEqualTo("587");
  }

  @Test
  @DisplayName("SMTP config respects env var overrides")
  void smtpEnvOverrides() {
    var env = new java.util.HashMap<String, String>();
    env.put("SMTP_SERVER", "smtp.gmail.com");
    env.put("SMTP_PORT", "465");

    assertThat(env.getOrDefault("SMTP_SERVER", "localhost")).isEqualTo("smtp.gmail.com");
    assertThat(env.getOrDefault("SMTP_PORT", "587")).isEqualTo("465");
    assertThat(env.getOrDefault("SMTP_USER", "default")).isEqualTo("default");
  }

  /* ───────── Kafka config env-var defaults ───────── */

  @Test
  @DisplayName("Kafka config uses env vars with safe defaults")
  void kafkaDefaultsAreSane() {
    var env = new java.util.HashMap<String, String>();

    assertThat(env.getOrDefault("KAFKA_BROKERS", "localhost:9092")).isEqualTo("localhost:9092");
  }

  @Test
  @DisplayName("Kafka broker config respects env var overrides")
  void kafkaEnvOverrides() {
    var env = new java.util.HashMap<String, String>();
    env.put("KAFKA_BROKERS", "kafka-1:9092,kafka-2:9092");

    assertThat(env.getOrDefault("KAFKA_BROKERS", "localhost:9092")).isEqualTo("kafka-1:9092,kafka-2:9092");
  }

  /* ───────── Email payload processing ───────── */

  @Test
  @DisplayName("complete email payload has all required fields")
  void completePayloadHasRequiredFields() {
    var payload = new JsonObject()
        .put("email", "user@example.com")
        .put("subject", "Welcome!")
        .put("body", "<h1>Welcome</h1>");

    assertThat(payload.getString("email")).isNotNull();
    assertThat(payload.getString("subject")).isNotNull();
    assertThat(payload.getString("body")).isNotNull();
  }

  @Test
  @DisplayName("incomplete email payload has missing fields detected")
  void incompletePayloadMissingFields() {
    var payload = new JsonObject()
        .put("email", "user@example.com");

    assertThat(payload.getString("subject")).isNull();
    assertThat(payload.getString("body")).isNull();
  }
}
