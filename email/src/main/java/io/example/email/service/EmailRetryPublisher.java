package io.example.email.service;

import io.example.email.observability.EmailMetrics;
import io.example.email.observability.EmailTracing;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes records to the unified retry topic and the dead-letter topic
 * (Phase 4 baseline).
 *
 * <p>A retry record carries the original enveloped payload plus metadata:
 * {@code _attempt}, {@code _retryAt} (epoch millis at which the retry worker
 * should attempt it), {@code _reason}, and the source coordinates
 * {@code _srcTopic/_srcPartition/_srcOffset}. A DLQ record carries
 * {@code _attempts}, {@code _reason}, {@code _failedAt} and the source
 * coordinates. The {@code event_id} is preserved both inside the payload and
 * as the Kafka message key, so the retry worker claims the same idempotency
 * key as the original record.
 *
 * <p>Callers must NOT commit the source offset until the retry/DLQ publish
 * succeeds: a failed publish surfaces as a failed future here.
 */
public class EmailRetryPublisher {

  public static final String RETRY_TOPIC = "email-service-topic-email-retry";
  public static final String DLQ_TOPIC = "email-service-topic-email-dlq";

  /** Maximum send attempts before a record moves to the DLQ. */
  public static final int MAX_ATTEMPTS = 5;

  public static final String META_ATTEMPT = "_attempt";
  public static final String META_RETRY_AT = "_retryAt";
  public static final String META_REASON = "_reason";
  public static final String META_SRC_TOPIC = "_srcTopic";
  public static final String META_SRC_PARTITION = "_srcPartition";
  public static final String META_SRC_OFFSET = "_srcOffset";
  public static final String META_ATTEMPTS = "_attempts";
  public static final String META_FAILED_AT = "_failedAt";

  private static final Logger log = LoggerFactory.getLogger(EmailRetryPublisher.class);

  private final KafkaProducer<String, String> producer;
  private final EmailMetrics metrics;
  private final EmailTracing tracing;

  public EmailRetryPublisher(KafkaProducer<String, String> producer) {
    this(producer, EmailMetrics.noop(), null);
  }

  public EmailRetryPublisher(KafkaProducer<String, String> producer, EmailMetrics metrics) {
    this(producer, metrics, null);
  }

  public EmailRetryPublisher(KafkaProducer<String, String> producer, EmailMetrics metrics, EmailTracing tracing) {
    this.producer = producer;
    this.metrics = metrics;
    this.tracing = tracing;
  }

  /** Returns a copy of {@code payload} with all retry/DLQ metadata fields removed. */
  public static JsonObject stripMetadata(JsonObject payload) {
    JsonObject clean = payload.copy();
    clean.remove(META_ATTEMPT);
    clean.remove(META_RETRY_AT);
    clean.remove(META_REASON);
    clean.remove(META_SRC_TOPIC);
    clean.remove(META_SRC_PARTITION);
    clean.remove(META_SRC_OFFSET);
    clean.remove(META_ATTEMPTS);
    clean.remove(META_FAILED_AT);
    return clean;
  }

  /**
   * Publishes the event to the retry topic for a later attempt.
   *
   * @param originalPayload the enveloped payload to redeliver (copied, never mutated)
   * @param eventId         the event id, used as the Kafka key
   * @param attempt         the attempt number this retry represents
   * @param retryAtMs       epoch millis at which the retry worker should attempt it
   * @param reason          why the record is being retried (e.g. {@code smtp_failure})
   */
  public Future<Void> publishRetry(JsonObject originalPayload, String eventId, int attempt,
                                   long retryAtMs, String reason,
                                   String srcTopic, int srcPartition, long srcOffset) {
    JsonObject payload = originalPayload.copy()
        .put(META_ATTEMPT, attempt)
        .put(META_RETRY_AT, retryAtMs)
        .put(META_REASON, reason)
        .put(META_SRC_TOPIC, srcTopic)
        .put(META_SRC_PARTITION, srcPartition)
        .put(META_SRC_OFFSET, srcOffset);
    return send(RETRY_TOPIC, eventId, payload, "retry");
  }

  /**
   * Publishes the event to the DLQ after all attempts were exhausted (or the
   * payload was permanently invalid). DLQ records are for operational review,
   * no consumer processes them.
   */
  public Future<Void> publishDlq(JsonObject originalPayload, String eventId, int attempts,
                                 String reason, String srcTopic, int srcPartition, long srcOffset) {
    JsonObject payload = originalPayload.copy()
        .put(META_ATTEMPTS, attempts)
        .put(META_REASON, reason)
        .put(META_FAILED_AT, System.currentTimeMillis())
        .put(META_SRC_TOPIC, srcTopic)
        .put(META_SRC_PARTITION, srcPartition)
        .put(META_SRC_OFFSET, srcOffset);
    return send(DLQ_TOPIC, eventId, payload, "DLQ");
  }

  private Future<Void> send(String topic, String key, JsonObject payload, String label) {
    KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key, payload.encode());
    // Keep the trace across topic hops (parented on the caller's consume span).
    if (tracing != null) {
      tracing.injectTraceparent(record);
    }
    Span span = tracing != null ? tracing.startSpan("email." + label + ".publish", SpanKind.CLIENT) : null;
    return producer.send(record)
        .<Void>mapEmpty()
        .onComplete(ar -> {
          if (tracing != null && span != null) {
            tracing.endSpan(span, ar.succeeded(), ar.failed() ? ar.cause().getMessage() : "published");
          }
          if (ar.succeeded()) {
            if ("retry".equalsIgnoreCase(label)) {
              metrics.recordRetried();
            } else if ("dlq".equalsIgnoreCase(label)) {
              metrics.recordDlq(payload.getString(META_REASON, "unknown"));
            }
            log.info("📤 Published {} record to {} (event_id {})", label, topic, key);
          } else {
            log.error("❌ Failed to publish {} record to {}", label, topic, ar.cause());
          }
        });
  }

  public void close() {
    if (producer != null) {
      producer.close();
    }
  }
}
