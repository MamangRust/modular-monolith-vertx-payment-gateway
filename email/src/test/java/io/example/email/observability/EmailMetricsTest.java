package io.example.email.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

class EmailMetricsTest {

  private static OpenTelemetry withReader(InMemoryMetricReader reader) {
    return OpenTelemetrySdk.builder()
        .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
        .build();
  }

  @Test
  void counters_recordExpectedValues() {
    InMemoryMetricReader reader = InMemoryMetricReader.create();
    EmailMetrics metrics = new EmailMetrics(withReader(reader));

    metrics.recordSent("auth.register");
    metrics.recordSent("auth.register");
    metrics.recordFailed("auth.register", "smtp");
    metrics.recordRetried();
    metrics.recordDlq("max_attempts");
    metrics.recordDuplicate();
    metrics.recordInvalid();
    metrics.recordClaimFailed();
    metrics.recordProcessingDuration(0.42);
    metrics.recordLag("email-service-topic-auth-register", 0, 7L);

    Collection<MetricData> all = reader.collectAllMetrics();
    assertThat(sum(all, "email_sent_total")).isEqualTo(2);
    assertThat(sum(all, "email_failed_total")).isEqualTo(1);
    assertThat(sum(all, "email_retried_total")).isEqualTo(1);
    assertThat(sum(all, "email_dlq_total")).isEqualTo(1);
    assertThat(sum(all, "email_duplicate_total")).isEqualTo(1);
    assertThat(sum(all, "email_invalid_event_total")).isEqualTo(1);
    assertThat(sum(all, "email_idempotency_claim_failed_total")).isEqualTo(1);
    assertThat(find(all, "email_processing_duration_seconds")).isPresent();
    assertThat(find(all, "kafka_consumer_lag")).isPresent();
  }

  @Test
  void lagGauge_recordsPerPartition() {
    InMemoryMetricReader reader = InMemoryMetricReader.create();
    EmailMetrics metrics = new EmailMetrics(withReader(reader));

    metrics.recordLag("email-service-topic-auth-register", 2, 150L);
    metrics.recordLag("email-service-topic-auth-register", 3, 5L);

    GaugeData<?> gauge = gauge(reader.collectAllMetrics(), "kafka_consumer_lag");
    assertThat(gauge.getPoints()).hasSize(2);
  }

  @Test
  void noop_shouldNeverThrow() {
    EmailMetrics noop = EmailMetrics.noop();
    noop.recordSent("x");
    noop.recordFailed("x", "smtp");
    noop.recordRetried();
    noop.recordDlq("reason");
    noop.recordDuplicate();
    noop.recordInvalid();
    noop.recordClaimFailed();
    noop.recordProcessingDuration(1.0);
    noop.recordLag("t", 0, 1L);
    noop.removeLag("t", 0);
    noop.clearLag();
    assertThat(noop).isNotNull();
  }

  private static long sum(Collection<MetricData> all, String name) {
    Optional<MetricData> metric = find(all, name);
    assertThat(metric).as("metric %s present", name).isPresent();
    Data<?> data = metric.get().getData();
    if (data instanceof SumData<?> sum) {
      long total = 0;
      for (PointData point : sum.getPoints()) {
        if (point instanceof LongPointData longPoint) {
          total += longPoint.getValue();
        }
      }
      return total;
    }
    return 0;
  }

  private static GaugeData<?> gauge(Collection<MetricData> all, String name) {
    Optional<MetricData> metric = find(all, name);
    assertThat(metric).as("metric %s present", name).isPresent();
    return (GaugeData<?>) metric.get().getData();
  }

  private static Optional<MetricData> find(Collection<MetricData> all, String name) {
    return all.stream().filter(metric -> metric.getName().equals(name)).findFirst();
  }
}
