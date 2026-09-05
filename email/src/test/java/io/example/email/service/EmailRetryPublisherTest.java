package io.example.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.kafka.client.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

@ExtendWith(MockitoExtension.class)
class EmailRetryPublisherTest {

  @Mock
  private KafkaProducer<String, String> producer;

  private EmailRetryPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new EmailRetryPublisher(producer);
    lenient().when(producer.send(any())).thenReturn(Future.succeededFuture((RecordMetadata) null));
  }

  private static JsonObject originalPayload() {
    return new JsonObject()
        .put("event_id", "evt-123")
        .put("schema_version", 1)
        .put("event_type", "auth.register")
        .put("occurred_at", "2026-01-01T00:00:00Z")
        .put("email", "buyer@example.com")
        .put("subject", "Welcome")
        .put("body", "<p>Welcome</p>");
  }

  private KafkaProducerRecord<String, String> capturedRecord() {
    ArgumentCaptor<KafkaProducerRecord<String, String>> captor =
        ArgumentCaptor.forClass(KafkaProducerRecord.class);
    verify(producer).send(captor.capture());
    return captor.getValue();
  }

  @Test
  void publishRetry_shouldSendToRetryTopicWithMetadataAndEventIdKey() {
    JsonObject original = originalPayload();

    Future<Void> result = publisher.publishRetry(original, "evt-123", 3, 1_700_000_000_000L, "smtp_failure",
        "email-service-topic-auth-register", 2, 41L);

    assertThat(result.succeeded()).as("Failed: %s", result.cause()).isTrue();
    KafkaProducerRecord<String, String> record = capturedRecord();
    assertThat(record.topic()).isEqualTo(EmailRetryPublisher.RETRY_TOPIC);
    assertThat(record.key()).isEqualTo("evt-123");

    JsonObject payload = new JsonObject(record.value());
    assertThat(payload.getInteger(EmailRetryPublisher.META_ATTEMPT)).isEqualTo(3);
    assertThat(payload.getLong(EmailRetryPublisher.META_RETRY_AT)).isEqualTo(1_700_000_000_000L);
    assertThat(payload.getString(EmailRetryPublisher.META_REASON)).isEqualTo("smtp_failure");
    assertThat(payload.getString(EmailRetryPublisher.META_SRC_TOPIC)).isEqualTo("email-service-topic-auth-register");
    assertThat(payload.getInteger(EmailRetryPublisher.META_SRC_PARTITION)).isEqualTo(2);
    assertThat(payload.getLong(EmailRetryPublisher.META_SRC_OFFSET)).isEqualTo(41L);

    // Original envelope + payload fields are preserved.
    assertThat(payload.getString("event_id")).isEqualTo("evt-123");
    assertThat(payload.getString("email")).isEqualTo("buyer@example.com");
    assertThat(payload.getString("subject")).isEqualTo("Welcome");
    assertThat(payload.getString("body")).isEqualTo("<p>Welcome</p>");
    // The input payload must not be mutated.
    assertThat(original.containsKey(EmailRetryPublisher.META_ATTEMPT)).isFalse();
  }

  @Test
  void publishDlq_shouldSendToDlqTopicWithAttemptsAndFailedAt() {
    Future<Void> result = publisher.publishDlq(originalPayload(), "evt-123", 5, "max_attempts",
        "email-service-topic-auth-register", 2, 41L);

    assertThat(result.succeeded()).as("Failed: %s", result.cause()).isTrue();
    KafkaProducerRecord<String, String> record = capturedRecord();
    assertThat(record.topic()).isEqualTo(EmailRetryPublisher.DLQ_TOPIC);
    assertThat(record.key()).isEqualTo("evt-123");

    JsonObject payload = new JsonObject(record.value());
    assertThat(payload.getInteger(EmailRetryPublisher.META_ATTEMPTS)).isEqualTo(5);
    assertThat(payload.getString(EmailRetryPublisher.META_REASON)).isEqualTo("max_attempts");
    assertThat(payload.getLong(EmailRetryPublisher.META_FAILED_AT)).isNotNull();
    assertThat(payload.getString(EmailRetryPublisher.META_SRC_TOPIC)).isEqualTo("email-service-topic-auth-register");
    assertThat(payload.getInteger(EmailRetryPublisher.META_SRC_PARTITION)).isEqualTo(2);
    assertThat(payload.getLong(EmailRetryPublisher.META_SRC_OFFSET)).isEqualTo(41L);
    assertThat(payload.getString("event_id")).isEqualTo("evt-123");
  }

  @Test
  void stripMetadata_shouldRemoveAllRetryMetadataAndKeepOriginal() {
    JsonObject retryPayload = originalPayload()
        .put(EmailRetryPublisher.META_ATTEMPT, 2)
        .put(EmailRetryPublisher.META_RETRY_AT, 1L)
        .put(EmailRetryPublisher.META_REASON, "smtp_failure")
        .put(EmailRetryPublisher.META_SRC_TOPIC, "t")
        .put(EmailRetryPublisher.META_SRC_PARTITION, 0)
        .put(EmailRetryPublisher.META_SRC_OFFSET, 1L)
        .put(EmailRetryPublisher.META_ATTEMPTS, 2)
        .put(EmailRetryPublisher.META_FAILED_AT, 1L);

    JsonObject clean = EmailRetryPublisher.stripMetadata(retryPayload);

    assertThat(clean.containsKey(EmailRetryPublisher.META_ATTEMPT)).isFalse();
    assertThat(clean.containsKey(EmailRetryPublisher.META_RETRY_AT)).isFalse();
    assertThat(clean.containsKey(EmailRetryPublisher.META_REASON)).isFalse();
    assertThat(clean.containsKey(EmailRetryPublisher.META_SRC_TOPIC)).isFalse();
    assertThat(clean.containsKey(EmailRetryPublisher.META_SRC_PARTITION)).isFalse();
    assertThat(clean.containsKey(EmailRetryPublisher.META_SRC_OFFSET)).isFalse();
    assertThat(clean.containsKey(EmailRetryPublisher.META_ATTEMPTS)).isFalse();
    assertThat(clean.containsKey(EmailRetryPublisher.META_FAILED_AT)).isFalse();
    assertThat(clean.getString("event_id")).isEqualTo("evt-123");
    assertThat(clean.getString("email")).isEqualTo("buyer@example.com");
    // Input must not be mutated.
    assertThat(retryPayload.containsKey(EmailRetryPublisher.META_ATTEMPT)).isTrue();
  }

  @Test
  void maxAttempts_shouldBeFive() {
    assertThat(EmailRetryPublisher.MAX_ATTEMPTS).isEqualTo(5);
  }

  @Test
  void publish_shouldPropagateProducerFailure() {
    when(producer.send(any()))
        .thenReturn(Future.failedFuture(new RuntimeException("broker down")));

    Future<Void> result = publisher.publishRetry(originalPayload(), "evt-123", 1, 1L, "smtp_failure",
        "email-service-topic-auth-register", 0, 1L);

    assertThat(result.failed()).isTrue();
    assertThat(result.cause()).hasMessage("broker down");
  }
}
