package io.example.common.service;

import io.example.common.event.EventEnvelope;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaService {
  private static final Logger log = LoggerFactory.getLogger(KafkaService.class);

  private static final String EMAIL_TOPIC_PREFIX = "email-service-topic-";

  private final KafkaProducer<String, String> producer;

  public KafkaService(KafkaProducer<String, String> producer) {
    this.producer = producer;
  }

  public Future<Void> sendMessage(String topic, String key, JsonObject value) {
    // Email topics carry the standard event envelope (event_id, schema_version,
    // event_type, occurred_at). Non-email topics pass through untouched so their
    // consumers keep seeing the payload they already expect.
    JsonObject payload = topic != null && topic.startsWith(EMAIL_TOPIC_PREFIX)
        ? EventEnvelope.withDefaults(value, EventEnvelope.eventTypeFromTopic(topic))
        : value;
    KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key, payload.encode());
    // Propagate the trace context (traceparent) so the email consumer can
    // continue the trace from producer through SMTP/DLQ.
    W3CTraceContextPropagator.getInstance().inject(Context.current(), record, (r, k, v) -> r.addHeader(k, v));
    return producer.send(record)
        .onSuccess(metadata -> log.info("📤 Message sent to topic {} (event_id {})",
            topic, payload.getString("event_id", "n/a")))
        .onFailure(err -> log.error("❌ Failed to send message to topic {}", topic, err))
        .mapEmpty();
  }

  public void close() {
    if (producer != null) {
      producer.close();
    }
  }
}
