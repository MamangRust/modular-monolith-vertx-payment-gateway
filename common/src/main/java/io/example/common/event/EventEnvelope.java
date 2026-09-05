package io.example.common.event;

import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard event envelope contract (Phase 2 baseline).
 *
 * <p>Every event published to the {@code email-service-topic-*} topics carries:
 * <ul>
 *   <li>{@code event_id} — stable business-event identity; never overwritten,
 *       so outbox replays and retries keep the same ID (safe for idempotency);</li>
 *   <li>{@code schema_version} — {@code 1};</li>
 *   <li>{@code event_type} — e.g. {@code auth.register};</li>
 *   <li>{@code occurred_at} — ISO-8601 timestamp.</li>
 * </ul>
 *
 * <p>Consumers must validate the envelope with {@link #isValid} before any SMTP
 * delivery; invalid payloads must never reach the mail client.
 */
public final class EventEnvelope {

  public static final int SCHEMA_VERSION = 1;
  private static final String EMAIL_TOPIC_PREFIX = "email-service-topic-";

  private static final String FIELD_EVENT_ID = "event_id";
  private static final String FIELD_SCHEMA_VERSION = "schema_version";
  private static final String FIELD_EVENT_TYPE = "event_type";
  private static final String FIELD_OCCURRED_AT = "occurred_at";

  private EventEnvelope() {
  }

  /**
   * Returns a copy of {@code payload} with the envelope fields added. An
   * existing {@code event_id} / {@code occurred_at} is preserved, so replays
   * keep the same business-event identity. The input is never mutated.
   */
  public static JsonObject withDefaults(JsonObject payload, String eventType) {
    JsonObject result = payload.copy();
    if (!result.containsKey(FIELD_EVENT_ID)) {
      result.put(FIELD_EVENT_ID, UUID.randomUUID().toString());
    }
    if (eventType != null && !eventType.isBlank()) {
      result.put(FIELD_EVENT_TYPE, eventType);
    }
    result.put(FIELD_SCHEMA_VERSION, SCHEMA_VERSION);
    if (!result.containsKey(FIELD_OCCURRED_AT)) {
      result.put(FIELD_OCCURRED_AT, Instant.now().toString());
    }
    return result;
  }

  /**
   * Derives the event type from an {@code email-service-topic-*} topic name,
   * e.g. {@code email-service-topic-auth-register} → {@code auth.register}.
   */
  public static String eventTypeFromTopic(String topic) {
    if (topic == null) {
      return null;
    }
    String name = topic.startsWith(EMAIL_TOPIC_PREFIX)
        ? topic.substring(EMAIL_TOPIC_PREFIX.length())
        : topic;
    return name.replace('-', '.');
  }

  /**
   * Validates that the payload carries the standard envelope plus the fields
   * required for SMTP delivery. Invalid payloads must never be sent.
   */
  public static boolean isValid(JsonObject payload) {
    if (payload == null) {
      return false;
    }
    try {
      return isNonBlank(payload.getString(FIELD_EVENT_ID))
          && payload.getInteger(FIELD_SCHEMA_VERSION, -1) == SCHEMA_VERSION
          && isNonBlank(payload.getString(FIELD_EVENT_TYPE))
          && isNonBlank(payload.getString("email"))
          && isNonBlank(payload.getString("subject"))
          && isNonBlank(payload.getString("body"));
    } catch (Exception e) {
      // Wrong JSON types (e.g. schema_version as a string) count as invalid.
      return false;
    }
  }

  private static boolean isNonBlank(String value) {
    return value != null && !value.isBlank();
  }
}
