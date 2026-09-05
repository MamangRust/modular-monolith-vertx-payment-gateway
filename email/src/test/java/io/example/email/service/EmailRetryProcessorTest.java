package io.example.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.kafka.client.producer.RecordMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.email.observability.EmailMetrics;
import io.example.email.service.EmailDedupGuard.ClaimResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

@ExtendWith(MockitoExtension.class)
class EmailRetryProcessorTest {

  private static final String SRC_TOPIC = "email-service-topic-auth-register";

  @Mock
  private KafkaConsumer<String, JsonObject> consumer;
  @Mock
  private MailClient mailClient;
  @Mock
  private EmailDedupGuard dedupGuard;
  @Mock
  private KafkaProducer<String, String> producer;

  private Vertx vertx;
  private EmailRetryPublisher publisher;
  private EmailRetryProcessor processor;

  @BeforeEach
  void setUp() {
    vertx = Vertx.vertx();
    when(consumer.commit(anyMap())).thenReturn(Future.succeededFuture());
    lenient().when(producer.send(any())).thenReturn(Future.succeededFuture((RecordMetadata) null));
    publisher = new EmailRetryPublisher(producer);
    processor = new EmailRetryProcessor(vertx, consumer, mailClient, dedupGuard, publisher,
        "no-reply@test.com", EmailMetrics.noop(), null);
    processor.start();
  }

  @AfterEach
  void tearDown() {
    processor.stop();
    vertx.close();
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

  private static JsonObject retryPayload(int attempt) {
    return originalPayload()
        .put(EmailRetryPublisher.META_ATTEMPT, attempt)
        .put(EmailRetryPublisher.META_RETRY_AT, System.currentTimeMillis() - 1_000L)
        .put(EmailRetryPublisher.META_REASON, "smtp_failure")
        .put(EmailRetryPublisher.META_SRC_TOPIC, SRC_TOPIC)
        .put(EmailRetryPublisher.META_SRC_PARTITION, 0)
        .put(EmailRetryPublisher.META_SRC_OFFSET, 7L);
  }

  private KafkaConsumerRecord<String, JsonObject> record(JsonObject value) {
    KafkaConsumerRecord<String, JsonObject> record = mock(KafkaConsumerRecord.class);
    when(record.topic()).thenReturn(EmailRetryPublisher.RETRY_TOPIC);
    when(record.partition()).thenReturn(0);
    when(record.offset()).thenReturn(5L);
    when(record.value()).thenReturn(value);
    return record;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private Handler<KafkaConsumerRecord<String, JsonObject>> captureHandler() {
    ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
    verify(consumer).handler(captor.capture());
    return captor.getValue();
  }

  private KafkaProducerRecord<String, String> capturedProducedRecord() {
    ArgumentCaptor<KafkaProducerRecord<String, String>> captor =
        ArgumentCaptor.forClass(KafkaProducerRecord.class);
    verify(producer, timeout(5000)).send(captor.capture());
    return captor.getValue();
  }

  @Test
  void sendSucceeds_shouldMarkSentAndCommitWithoutRepublish() {
    when(dedupGuard.claim(anyString(), anyString(), anyInt(), anyLong()))
        .thenReturn(Future.succeededFuture(ClaimResult.CLAIMED));
    when(dedupGuard.markSent(anyString(), anyString(), anyInt(), anyLong()))
        .thenReturn(Future.succeededFuture());
    when(mailClient.sendMail(any())).thenReturn(Future.succeededFuture());

    captureHandler().handle(record(retryPayload(2)));

    verify(dedupGuard, timeout(5000)).markSent("evt-123", EmailRetryPublisher.RETRY_TOPIC, 0, 5L);
    verify(consumer, timeout(5000)).commit(anyMap());
    verify(producer, never()).send(any());
  }

  @Test
  void smtpFailureBelowMax_shouldRepublishToRetryTopicWithIncrementedAttempt() {
    when(dedupGuard.claim(anyString(), anyString(), anyInt(), anyLong()))
        .thenReturn(Future.succeededFuture(ClaimResult.CLAIMED));
    when(dedupGuard.release(anyString(), anyString(), anyInt(), anyLong()))
        .thenReturn(Future.succeededFuture());
    when(mailClient.sendMail(any()))
        .thenReturn(Future.failedFuture(new RuntimeException("smtp down")));

    captureHandler().handle(record(retryPayload(2)));

    KafkaProducerRecord<String, String> produced = capturedProducedRecord();
    assertThat(produced.topic()).isEqualTo(EmailRetryPublisher.RETRY_TOPIC);
    assertThat(produced.key()).isEqualTo("evt-123");
    JsonObject outbound = new JsonObject(produced.value());
    assertThat(outbound.getInteger(EmailRetryPublisher.META_ATTEMPT)).isEqualTo(3);
    assertThat(outbound.getLong(EmailRetryPublisher.META_RETRY_AT)).isGreaterThan(System.currentTimeMillis());
    verify(dedupGuard, timeout(5000)).release("evt-123", EmailRetryPublisher.RETRY_TOPIC, 0, 5L);
    verify(consumer, timeout(5000)).commit(anyMap());
  }

  @Test
  void smtpFailureAtMaxAttempts_shouldMoveToDlq() {
    when(dedupGuard.claim(anyString(), anyString(), anyInt(), anyLong()))
        .thenReturn(Future.succeededFuture(ClaimResult.CLAIMED));
    when(dedupGuard.release(anyString(), anyString(), anyInt(), anyLong()))
        .thenReturn(Future.succeededFuture());
    when(mailClient.sendMail(any()))
        .thenReturn(Future.failedFuture(new RuntimeException("smtp down")));

    captureHandler().handle(record(retryPayload(EmailRetryPublisher.MAX_ATTEMPTS)));

    KafkaProducerRecord<String, String> produced = capturedProducedRecord();
    assertThat(produced.topic()).isEqualTo(EmailRetryPublisher.DLQ_TOPIC);
    JsonObject outbound = new JsonObject(produced.value());
    assertThat(outbound.getInteger(EmailRetryPublisher.META_ATTEMPTS))
        .isEqualTo(EmailRetryPublisher.MAX_ATTEMPTS);
    assertThat(outbound.getString(EmailRetryPublisher.META_REASON)).isEqualTo("smtp_failure");
    verify(consumer, timeout(5000)).commit(anyMap());
  }

  @Test
  void invalidEnvelope_shouldMoveToDlqWithoutSending() {
    captureHandler().handle(record(new JsonObject().put("email", "buyer@example.com")));

    KafkaProducerRecord<String, String> produced = capturedProducedRecord();
    assertThat(produced.topic()).isEqualTo(EmailRetryPublisher.DLQ_TOPIC);
    JsonObject outbound = new JsonObject(produced.value());
    assertThat(outbound.getString(EmailRetryPublisher.META_REASON)).isEqualTo("invalid_envelope");
    verify(mailClient, never()).sendMail(any());
    verify(consumer, timeout(5000)).commit(anyMap());
  }

  @Test
  void duplicate_shouldCommitWithoutSendingOrRepublishing() {
    when(dedupGuard.claim(anyString(), anyString(), anyInt(), anyLong()))
        .thenReturn(Future.succeededFuture(ClaimResult.DUPLICATE));

    captureHandler().handle(record(retryPayload(1)));

    verify(consumer, timeout(5000)).commit(anyMap());
    verify(mailClient, never()).sendMail(any());
    verify(producer, never()).send(any());
  }
}
