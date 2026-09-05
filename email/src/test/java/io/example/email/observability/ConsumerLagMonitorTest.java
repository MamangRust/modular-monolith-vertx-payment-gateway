package io.example.email.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class ConsumerLagMonitorTest {

  @Mock
  private KafkaConsumer<String, JsonObject> consumer;

  private Vertx vertx;
  private InMemoryMetricReader reader;
  private EmailMetrics metrics;

  @BeforeEach
  void setUp() {
    vertx = Vertx.vertx();
    reader = InMemoryMetricReader.create();
    OpenTelemetry otel = OpenTelemetrySdk.builder()
        .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
        .build();
    metrics = new EmailMetrics(otel);
  }

  @AfterEach
  void tearDown() {
    vertx.close();
  }

  @Test
  void poll_shouldExposeLagPerPartition() throws Exception {
    TopicPartition tp = new TopicPartition("email-service-topic-auth-register", 0);
    when(consumer.assignment()).thenReturn(Future.succeededFuture(Set.of(tp)));
    when(consumer.endOffsets(anySet())).thenReturn(Future.succeededFuture(Map.of(tp, 100L)));
    when(consumer.committed(any())).thenReturn(Future.succeededFuture(new OffsetAndMetadata().setOffset(40L)));

    ConsumerLagMonitor monitor = new ConsumerLagMonitor(vertx, consumer, metrics, 100);
    monitor.start();
    try {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while (System.nanoTime() < deadline) {
        GaugeData<?> gauge = findGauge();
        if (gauge != null && !gauge.getPoints().isEmpty()) {
          PointData point = gauge.getPoints().iterator().next();
          if (point instanceof LongPointData longPoint && longPoint.getValue() == 60L) {
            return; // lag = endOffsets(100) - committed(40)
          }
        }
        Thread.sleep(50);
      }
      assertThat(findGauge()).as("lag gauge populated within 5s").isNotNull();
    } finally {
      monitor.stop();
    }
  }

  @Test
  void poll_shouldSkipPartitionWithoutCommittedOffset() throws Exception {
    TopicPartition tp = new TopicPartition("email-service-topic-auth-register", 0);
    when(consumer.assignment()).thenReturn(Future.succeededFuture(Set.of(tp)));
    when(consumer.endOffsets(anySet())).thenReturn(Future.succeededFuture(Map.of(tp, 100L)));
    when(consumer.committed(any())).thenReturn(Future.succeededFuture(null));

    ConsumerLagMonitor monitor = new ConsumerLagMonitor(vertx, consumer, metrics, 100);
    monitor.start();
    try {
      Thread.sleep(400);
      GaugeData<?> gauge = findGauge();
      assertThat(gauge).isNull(); // nothing recorded, so the gauge never materializes
    } finally {
      monitor.stop();
    }
  }

  private GaugeData<?> findGauge() {
    for (MetricData metric : reader.collectAllMetrics()) {
      if (metric.getName().equals("kafka_consumer_lag") && metric.getData() instanceof GaugeData<?> gauge) {
        return gauge;
      }
    }
    return null;
  }
}
