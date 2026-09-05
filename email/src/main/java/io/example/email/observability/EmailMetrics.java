package io.example.email.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard email reliability metrics (Phase 5 baseline, meter {@code email-service}).
 *
 * <p>Exposes the uniform contract used by the alert rules and dashboard:
 * <ul>
 *   <li>{@code email_sent_total}, {@code email_failed_total}, {@code email_retried_total},
 *       {@code email_dlq_total}, {@code email_duplicate_total}, {@code email_invalid_event_total}
 *       — counters;</li>
 *   <li>{@code email_idempotency_claim_failed_total} — Redis claim failures (fail-open active);</li>
 *   <li>{@code email_processing_duration_seconds} — histogram, per processing attempt;</li>
 *   <li>{@code kafka_consumer_lag} — gauge per {@code topic}/{@code partition}, fed by
 *       {@link ConsumerLagMonitor}.</li>
 * </ul>
 */
public class EmailMetrics {

  public static final String METER_NAME = "email-service";

  private static final AttributeKey<String> EVENT_TYPE_KEY = AttributeKey.stringKey("event_type");
  private static final AttributeKey<String> REASON_KEY = AttributeKey.stringKey("reason");
  private static final AttributeKey<String> TOPIC_KEY = AttributeKey.stringKey("topic");
  private static final AttributeKey<Long> PARTITION_KEY = AttributeKey.longKey("partition");
  private static final AttributeKey<String> GROUP_KEY = AttributeKey.stringKey("group");

  /** Consumer group monitored by the lag gauge (matches the alert filter). */
  public static final String CONSUMER_GROUP = "email-service-group";

  private final boolean enabled;
  private final LongCounter sentCounter;
  private final LongCounter failedCounter;
  private final LongCounter retriedCounter;
  private final LongCounter dlqCounter;
  private final LongCounter duplicateCounter;
  private final LongCounter invalidCounter;
  private final LongCounter claimFailedCounter;
  private final DoubleHistogram processingDuration;
  private final Map<String, Long> lagByPartition = new ConcurrentHashMap<>();

  /**
   * @param openTelemetry nullable — {@code null} produces a no-op instance (used by unit tests
   *                      and the legacy {@code EmailDedupGuard} constructor).
   */
  public EmailMetrics(OpenTelemetry openTelemetry) {
    this(openTelemetry, CONSUMER_GROUP);
  }

  /**
   * @param consumerGroup group label on the {@code kafka_consumer_lag} gauge, so the
   *                      {@code EmailConsumerLagHigh}/{@code EmailConsumerLagMissing} alerts can
   *                      filter on {@code group="email-service-group"}.
   */
  public EmailMetrics(OpenTelemetry openTelemetry, String consumerGroup) {
    if (openTelemetry == null) {
      this.enabled = false;
      this.sentCounter = null;
      this.failedCounter = null;
      this.retriedCounter = null;
      this.dlqCounter = null;
      this.duplicateCounter = null;
      this.invalidCounter = null;
      this.claimFailedCounter = null;
      this.processingDuration = null;
      return;
    }
    this.enabled = true;
    Meter meter = openTelemetry.getMeter(METER_NAME);

    this.sentCounter = meter.counterBuilder("email_sent_total")
        .setDescription("Total emails successfully delivered via SMTP")
        .build();
    this.failedCounter = meter.counterBuilder("email_failed_total")
        .setDescription("Total email send attempts that failed")
        .build();
    this.retriedCounter = meter.counterBuilder("email_retried_total")
        .setDescription("Total records published to the email retry topic")
        .build();
    this.dlqCounter = meter.counterBuilder("email_dlq_total")
        .setDescription("Total records moved to the email DLQ")
        .build();
    this.duplicateCounter = meter.counterBuilder("email_duplicate_total")
        .setDescription("Total duplicate records skipped (already SENT)")
        .build();
    this.invalidCounter = meter.counterBuilder("email_invalid_event_total")
        .setDescription("Total records with an invalid event envelope")
        .build();
    this.claimFailedCounter = meter.counterBuilder("email_idempotency_claim_failed_total")
        .setDescription("Total Redis idempotency claim failures (fail-open active)")
        .build();

    this.processingDuration = meter.histogramBuilder("email_processing_duration_seconds")
        .setDescription("Email processing duration per record attempt")
        .setUnit("s")
        .setExplicitBucketBoundariesAdvice(Arrays.asList(0.01, 0.05, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 30.0, 60.0))
        .build();

    meter.gaugeBuilder("kafka_consumer_lag")
        .setDescription("Current consumer lag per topic partition")
        .setUnit("messages")
        .buildWithCallback(observable ->
            lagByPartition.forEach((key, lag) -> {
              int idx = key.lastIndexOf(':');
              observable.record(lag, Attributes.of(
                  TOPIC_KEY, key.substring(0, idx),
                  PARTITION_KEY, Long.parseLong(key.substring(idx + 1)),
                  GROUP_KEY, consumerGroup));
            }));
  }

  public static EmailMetrics noop() {
    return new EmailMetrics(null);
  }

  public void recordSent(String eventType) {
    if (enabled) {
      sentCounter.add(1, Attributes.of(EVENT_TYPE_KEY, eventTypeOrUnknown(eventType)));
    }
  }

  public void recordFailed(String eventType, String reason) {
    if (enabled) {
      failedCounter.add(1, Attributes.of(
          EVENT_TYPE_KEY, eventTypeOrUnknown(eventType),
          REASON_KEY, reason == null || reason.isBlank() ? "unknown" : reason));
    }
  }

  public void recordRetried() {
    if (enabled) {
      retriedCounter.add(1);
    }
  }

  public void recordDlq(String reason) {
    if (enabled) {
      dlqCounter.add(1, Attributes.of(REASON_KEY, reason == null || reason.isBlank() ? "unknown" : reason));
    }
  }

  public void recordDuplicate() {
    if (enabled) {
      duplicateCounter.add(1);
    }
  }

  public void recordInvalid() {
    if (enabled) {
      invalidCounter.add(1);
    }
  }

  public void recordClaimFailed() {
    if (enabled) {
      claimFailedCounter.add(1);
    }
  }

  public void recordProcessingDuration(double seconds) {
    if (enabled) {
      processingDuration.record(seconds);
    }
  }

  public void recordLag(String topic, int partition, long lag) {
    if (enabled) {
      lagByPartition.put(topic + ":" + partition, lag);
    }
  }

  public void removeLag(String topic, int partition) {
    if (enabled) {
      lagByPartition.remove(topic + ":" + partition);
    }
  }

  public void clearLag() {
    if (enabled) {
      lagByPartition.clear();
    }
  }

  private static String eventTypeOrUnknown(String eventType) {
    return eventType == null || eventType.isBlank() ? "unknown" : eventType;
  }
}
