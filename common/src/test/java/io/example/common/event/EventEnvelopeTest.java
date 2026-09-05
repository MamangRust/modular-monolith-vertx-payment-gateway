package io.example.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

class EventEnvelopeTest {

  @Test
  void withDefaults_addsEnvelopeFields() {
    JsonObject payload = new JsonObject()
        .put("email", "a@b.c")
        .put("subject", "Hi")
        .put("body", "<p>Hi</p>");

    JsonObject enveloped = EventEnvelope.withDefaults(payload, "auth.register");

    assertThat(enveloped.getString("event_id")).isNotBlank();
    assertThat(enveloped.getInteger("schema_version")).isEqualTo(1);
    assertThat(enveloped.getString("event_type")).isEqualTo("auth.register");
    assertThat(enveloped.getString("occurred_at")).isNotBlank();
    assertThat(enveloped.getString("email")).isEqualTo("a@b.c");
    assertThat(enveloped.getString("subject")).isEqualTo("Hi");
    assertThat(enveloped.getString("body")).isEqualTo("<p>Hi</p>");
  }

  @Test
  void withDefaults_preservesExistingEventId() {
    JsonObject payload = new JsonObject()
        .put("event_id", "evt-123")
        .put("email", "a@b.c")
        .put("subject", "Hi")
        .put("body", "<p>Hi</p>");

    JsonObject enveloped = EventEnvelope.withDefaults(payload, "auth.register");

    assertThat(enveloped.getString("event_id")).isEqualTo("evt-123");
  }

  @Test
  void withDefaults_doesNotMutateInput() {
    JsonObject payload = new JsonObject().put("email", "a@b.c");

    EventEnvelope.withDefaults(payload, "auth.register");

    assertThat(payload.containsKey("event_id")).isFalse();
    assertThat(payload.containsKey("schema_version")).isFalse();
    assertThat(payload.containsKey("event_type")).isFalse();
    assertThat(payload.containsKey("occurred_at")).isFalse();
  }

  @Test
  void eventTypeFromTopic_derivesDotSeparatedType() {
    assertThat(EventEnvelope.eventTypeFromTopic("email-service-topic-auth-register"))
        .isEqualTo("auth.register");
    assertThat(EventEnvelope.eventTypeFromTopic("email-service-topic-merchant-document-update-status"))
        .isEqualTo("merchant.document.update.status");
  }

  @Test
  void isValid_acceptsCompleteEnvelope() {
    JsonObject payload = new JsonObject()
        .put("event_id", "evt-123")
        .put("schema_version", 1)
        .put("event_type", "auth.register")
        .put("email", "a@b.c")
        .put("subject", "Hi")
        .put("body", "<p>Hi</p>");

    assertThat(EventEnvelope.isValid(payload)).isTrue();
  }

  @Test
  void isValid_rejectsPayloadWithoutEnvelope() {
    JsonObject payload = new JsonObject()
        .put("email", "a@b.c")
        .put("subject", "Hi")
        .put("body", "<p>Hi</p>");

    assertThat(EventEnvelope.isValid(payload)).isFalse();
  }

  @Test
  void isValid_rejectsWrongSchemaVersion() {
    JsonObject payload = new JsonObject()
        .put("event_id", "evt-123")
        .put("schema_version", 2)
        .put("event_type", "auth.register")
        .put("email", "a@b.c")
        .put("subject", "Hi")
        .put("body", "<p>Hi</p>");

    assertThat(EventEnvelope.isValid(payload)).isFalse();
  }

  @Test
  void isValid_rejectsMissingEventType() {
    JsonObject payload = new JsonObject()
        .put("event_id", "evt-123")
        .put("schema_version", 1)
        .put("email", "a@b.c")
        .put("subject", "Hi")
        .put("body", "<p>Hi</p>");

    assertThat(EventEnvelope.isValid(payload)).isFalse();
  }

  @Test
  void isValid_rejectsMissingEmail() {
    JsonObject payload = new JsonObject()
        .put("event_id", "evt-123")
        .put("schema_version", 1)
        .put("event_type", "auth.register")
        .put("subject", "Hi")
        .put("body", "<p>Hi</p>");

    assertThat(EventEnvelope.isValid(payload)).isFalse();
  }

  @Test
  void isValid_rejectsNull() {
    assertThat(EventEnvelope.isValid(null)).isFalse();
  }
}
