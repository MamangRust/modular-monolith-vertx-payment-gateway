package io.example.email.observability;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Periodically refreshes the {@code kafka_consumer_lag} gauge per topic
 * partition (Phase 5 baseline). Lag = {@code endOffsets} − committed offset
 * (both queried from the broker), poll interval configurable via
 * {@code EMAIL_LAG_POLL_MS} (default 15s). Used by the
 * {@code EmailConsumerLagHigh} / {@code EmailConsumerLagMissing} alerts.
 */
public class ConsumerLagMonitor {

  private static final Logger log = LoggerFactory.getLogger(ConsumerLagMonitor.class);

  private final Vertx vertx;
  private final KafkaConsumer<String, JsonObject> consumer;
  private final EmailMetrics metrics;
  private final long pollIntervalMs;
  private long timerId = -1;

  public ConsumerLagMonitor(Vertx vertx, KafkaConsumer<String, JsonObject> consumer,
                            EmailMetrics metrics, long pollIntervalMs) {
    this.vertx = vertx;
    this.consumer = consumer;
    this.metrics = metrics;
    this.pollIntervalMs = pollIntervalMs;
  }

  public void start() {
    if (timerId != -1) {
      return;
    }
    timerId = vertx.setPeriodic(pollIntervalMs, id -> poll());
  }

  public void stop() {
    if (timerId != -1) {
      vertx.cancelTimer(timerId);
      timerId = -1;
    }
  }

  private void poll() {
    consumer.assignment()
        .compose(partitions -> {
          if (partitions == null || partitions.isEmpty()) {
            metrics.clearLag();
            return Future.succeededFuture();
          }
          return consumer.endOffsets(partitions).compose(endOffsets -> {
            List<Future<Void>> checks = new ArrayList<>();
            for (TopicPartition tp : partitions) {
              checks.add(consumer.committed(tp)
                  .<Void>map(committed -> {
                    long end = endOffsets.getOrDefault(tp, -1L);
                    long committedOffset = committed == null ? -1L : committed.getOffset();
                    if (end >= 0 && committedOffset >= 0) {
                      metrics.recordLag(tp.getTopic(), tp.getPartition(),
                          Math.max(0L, end - committedOffset));
                    } else {
                      metrics.removeLag(tp.getTopic(), tp.getPartition());
                    }
                    return null;
                  })
                  .otherwise(err -> {
                    metrics.removeLag(tp.getTopic(), tp.getPartition());
                    return null;
                  }));
            }
            return Future.all(checks).mapEmpty();
          });
        })
        .onFailure(err -> log.warn("⚠️ Consumer lag poll failed: {}", err.getMessage()));
  }
}
