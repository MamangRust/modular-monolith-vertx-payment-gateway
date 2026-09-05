package io.example.email.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.producer.KafkaHeader;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracing helpers for the email pipeline (Phase 5 baseline).
 *
 * <p>Consumers start a {@code email.consume} span whose parent is the
 * {@code traceparent} header propagated by the producer (via
 * {@link #injectTraceparent}), so an event is traceable from producer through
 * SMTP/DLQ. Retry and DLQ publishes inject the current context into the record
 * headers, keeping the trace across topic hops.
 */
public class EmailTracing {

  private static final String TRACEPARENT_HEADER = "traceparent";

  private final Tracer tracer;
  private final TextMapPropagator propagator;

  public EmailTracing(OpenTelemetry openTelemetry) {
    this.tracer = openTelemetry.getTracer("email-service");
    this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
  }

  /** Starts a consumer span for {@code record}, parenting it on its traceparent header. */
  public Span startConsumeSpan(KafkaConsumerRecord<String, JsonObject> record, String operation) {
    SpanBuilder builder = tracer.spanBuilder(operation).setSpanKind(SpanKind.CONSUMER);
    String traceparent = headerValue(record, TRACEPARENT_HEADER);
    if (traceparent != null && !traceparent.isBlank()) {
      builder.setParent(propagator.extract(Context.current(), record, HEADER_GETTER));
    }
    return builder
        .setAttribute("messaging.system", "kafka")
        .setAttribute("messaging.destination.name", record.topic())
        .setAttribute("messaging.kafka.partition", record.partition())
        .setAttribute("messaging.kafka.offset", record.offset())
        .startSpan();
  }

  /** Starts a span from the current context (caller must scope the context first). */
  public Span startSpan(String operation, SpanKind kind) {
    return tracer.spanBuilder(operation).setSpanKind(kind).startSpan();
  }

  public void endSpan(Span span, boolean success, String message) {
    if (success) {
      span.setStatus(StatusCode.OK);
    } else {
      span.setStatus(StatusCode.ERROR, message);
    }
    span.end();
  }

  /** Injects the current context as a {@code traceparent} header on the record. */
  public void injectTraceparent(KafkaProducerRecord<?, ?> record) {
    propagator.inject(Context.current(), record, (r, key, value) -> r.addHeader(key, value));
  }

  private static String headerValue(KafkaConsumerRecord<String, JsonObject> record, String key) {
    if (record.headers() == null) {
      return null;
    }
    for (KafkaHeader header : record.headers()) {
      if (key.equals(header.key())) {
        return header.value() == null ? null : header.value().toString();
      }
    }
    return null;
  }

  private static final TextMapGetter<KafkaConsumerRecord<String, JsonObject>> HEADER_GETTER =
      new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(KafkaConsumerRecord<String, JsonObject> carrier) {
          List<String> keys = new ArrayList<>();
          if (carrier.headers() != null) {
            for (KafkaHeader header : carrier.headers()) {
              keys.add(header.key());
            }
          }
          return keys;
        }

        @Override
        public String get(KafkaConsumerRecord<String, JsonObject> carrier, String key) {
          return headerValue(carrier, key);
        }
      };
}
