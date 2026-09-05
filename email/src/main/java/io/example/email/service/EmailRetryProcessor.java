package io.example.email.service;

import io.example.common.event.EventEnvelope;
import io.example.email.observability.EmailMetrics;
import io.example.email.observability.EmailTracing;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Retry worker for the email service (Phase 4/5 baseline). Consumes the unified
 * retry topic ({@value EmailRetryPublisher#RETRY_TOPIC}) and attempts each
 * email again, escalating {@code _attempt} until
 * {@link EmailRetryPublisher#MAX_ATTEMPTS}, after which the record is moved to
 * the DLQ.
 *
 * <p>Each retry record carries the original enveloped payload plus metadata
 * ({@code _attempt}, {@code _retryAt}, {@code _reason}, source coordinates).
 * The worker waits in-process until {@code _retryAt} (offset uncommitted while
 * waiting), then re-claims the <b>same</b> idempotency key as the original
 * record — a duplicate already sent by another path is skipped.
 *
 * <p>The retry offset is committed ONLY after a terminal outcome: email
 * delivered, record proven duplicate, or the retry/DLQ republish succeeded.
 * Publish failures keep the offset uncommitted and retry in-process, so a
 * crash during the window redelivers the record on restart (at-least-once).
 *
 * <p>Phase 5: every outcome feeds the {@code email_*} metrics and the consume
 * trace continues into SMTP and retry/DLQ publishes.
 */
public class EmailRetryProcessor {

  private static final Logger log = LoggerFactory.getLogger(EmailRetryProcessor.class);

  /** Backoff floor/cap for the in-process wait between retry hops. */
  private static final long RETRY_BASE_MS = 2_000;
  private static final long RETRY_MAX_MS = 300_000;

  private final Vertx vertx;
  private final KafkaConsumer<String, JsonObject> consumer;
  private final MailClient mailClient;
  private final EmailDedupGuard dedupGuard;
  private final EmailRetryPublisher publisher;
  private final String smtpFrom;
  private final EmailMetrics metrics;
  private final EmailTracing tracing;

  // Serialized processing chain: retry records are handled one at a time so an
  // offset is only committed after a terminal outcome for that record.
  private Future<Void> processingChain = Future.succeededFuture();
  private final Set<Long> retryTimers = new HashSet<>();

  public EmailRetryProcessor(Vertx vertx,
                             KafkaConsumer<String, JsonObject> consumer,
                             MailClient mailClient,
                             EmailDedupGuard dedupGuard,
                             EmailRetryPublisher publisher,
                             String smtpFrom,
                             EmailMetrics metrics,
                             EmailTracing tracing) {
    this.vertx = vertx;
    this.consumer = consumer;
    this.mailClient = mailClient;
    this.dedupGuard = dedupGuard;
    this.publisher = publisher;
    this.smtpFrom = smtpFrom;
    this.metrics = metrics;
    this.tracing = tracing;
  }

  /** Registers the consumer handler. Records are processed serially. */
  public void start() {
    consumer.handler(record -> processingChain = processingChain.compose(v -> processRecord(record)));
  }

  private Future<Void> processRecord(KafkaConsumerRecord<String, JsonObject> record) {
    Span span = tracing != null ? tracing.startConsumeSpan(record, "email.consume") : null;
    Context otelContext = span != null ? span.storeInContext(Context.current()) : Context.current();
    long startNanos = System.nanoTime();
    Future<Void> result;
    try (Scope ignored = otelContext.makeCurrent()) {
      result = doProcessRecord(record, otelContext);
    }
    return result.onComplete(v -> {
      if (tracing != null && span != null) {
        tracing.endSpan(span, v.succeeded(), v.failed() ? v.cause().getMessage() : "processed");
      }
      metrics.recordProcessingDuration((System.nanoTime() - startNanos) / 1_000_000_000.0);
    });
  }

  private Future<Void> doProcessRecord(KafkaConsumerRecord<String, JsonObject> record, Context otelContext) {
    String where = "topic=" + record.topic() + " partition=" + record.partition() + " offset=" + record.offset();
    try {
      JsonObject payload = record.value();
      JsonObject original = EmailRetryPublisher.stripMetadata(payload);
      int attempt = payload.getInteger(EmailRetryPublisher.META_ATTEMPT, 1);
      long retryAt = payload.getLong(EmailRetryPublisher.META_RETRY_AT, 0L);
      String reason = payload.getString(EmailRetryPublisher.META_REASON, "retry");

      if (!EventEnvelope.isValid(original)) {
        log.error("⚠️ Invalid envelope in retry record, moving to DLQ: {}", where);
        metrics.recordInvalid();
        return inContext(otelContext, () -> publisher.publishDlq(original, original.getString("event_id"),
                attempt, "invalid_envelope", record.topic(), record.partition(), record.offset()))
            .compose(v -> commit(record))
            .recover(err -> retryInProcess(record, RETRY_BASE_MS));
      }

      String eventId = original.getString("event_id");
      return waitUntil(retryAt)
          .compose(v -> dedupGuard.claim(eventId, record.topic(), record.partition(), record.offset()))
          .compose(result -> {
            switch (result) {
              case DUPLICATE:
                // Already delivered by another path — skip, commit.
                log.info("⏭️ Retry record duplicate (already SENT), committing without sending: {}", where);
                metrics.recordDuplicate();
                return commit(record);
              case BUSY:
                // Claim held elsewhere; defer again via the retry topic.
                log.info("⏳ Retry claim busy, deferring via retry topic: {}", where);
                return inContext(otelContext, () -> publisher.publishRetry(original, eventId, attempt,
                        System.currentTimeMillis() + RETRY_BASE_MS, "busy",
                        record.topic(), record.partition(), record.offset()))
                    .compose(v -> commit(record))
                    .recover(err -> retryInProcess(record, RETRY_BASE_MS));
              case CLAIMED:
              default:
                return sendEmail(original, otelContext)
                    .compose(v -> dedupGuard.markSent(eventId, record.topic(), record.partition(), record.offset()))
                    .compose(v -> commit(record))
                    .recover(err -> {
                      // SMTP failure: release the claim and escalate. The retry
                      // offset is only committed once the republish succeeds.
                      log.error("❌ Retry SMTP send failed (attempt {}): {}", attempt, where, err);
                      metrics.recordFailed(original.getString("event_type"), "smtp");
                      return dedupGuard.release(eventId, record.topic(), record.partition(), record.offset())
                          .compose(v -> {
                            if (attempt >= EmailRetryPublisher.MAX_ATTEMPTS) {
                              log.warn("⚠️ Max attempts ({}) reached, moving to DLQ: {}",
                                  EmailRetryPublisher.MAX_ATTEMPTS, where);
                              return inContext(otelContext, () -> publisher.publishDlq(original, eventId, attempt,
                                      reason, record.topic(), record.partition(), record.offset()))
                                  .compose(dlq -> commit(record))
                                  .recover(pubErr -> retryInProcess(record, RETRY_BASE_MS));
                            }
                            long nextRetryAt = System.currentTimeMillis() + backoff(attempt);
                            return inContext(otelContext, () -> publisher.publishRetry(original, eventId,
                                    attempt + 1, nextRetryAt, reason,
                                    record.topic(), record.partition(), record.offset()))
                                .compose(r -> commit(record))
                                .recover(pubErr -> retryInProcess(record, RETRY_BASE_MS));
                          });
                    });
            }
          });
    } catch (Exception e) {
      // Never let a synchronous error break the chain: keep the offset
      // uncommitted and retry in-process.
      log.error("❌ Unexpected error in retry processor, retrying in-process (offset NOT committed): {}", where, e);
      metrics.recordFailed("unknown", "unexpected");
      return retryInProcess(record, RETRY_BASE_MS);
    }
  }

  private Future<Void> sendEmail(JsonObject payload, Context otelContext) {
    MailMessage message = new MailMessage()
        .setFrom(smtpFrom)
        .setTo(payload.getString("email"))
        .setSubject(payload.getString("subject"))
        .setHtml(payload.getString("body")); // Using setHtml as most notification bodies are HTML

    Span span = startSmtpSpan(otelContext);
    return mailClient.sendMail(message)
        .<Void>mapEmpty()
        .onComplete(ar -> {
          if (tracing != null && span != null) {
            tracing.endSpan(span, ar.succeeded(), ar.failed() ? ar.cause().getMessage() : "sent");
          }
          if (ar.succeeded()) {
            metrics.recordSent(payload.getString("event_type"));
            log.info("✅ Retry email successfully sent to {}", payload.getString("email"));
          }
        });
  }

  private Span startSmtpSpan(Context otelContext) {
    if (tracing == null) {
      return null;
    }
    try (Scope ignored = otelContext.makeCurrent()) {
      return tracing.startSpan("email.smtp.send", SpanKind.CLIENT);
    }
  }

  /** Runs {@code action} with the consume context current (parent for spans/injection). */
  private <T> Future<T> inContext(Context otelContext, Supplier<Future<T>> action) {
    try (Scope ignored = otelContext.makeCurrent()) {
      return action.get();
    }
  }

  /** Waits in-process until {@code retryAtMs} (no-op if already past). */
  private Future<Void> waitUntil(long retryAtMs) {
    // Vert.x rejects timers with delay < 1 ms, so clamp to 1 ms minimum.
    long delay = Math.max(1L, retryAtMs - System.currentTimeMillis());
    Promise<Void> promise = Promise.promise();
    long timerId = vertx.setTimer(delay, id -> {
      retryTimers.remove(id);
      promise.complete();
    });
    retryTimers.add(timerId);
    return promise.future();
  }

  /** Capped exponential backoff for the next retry hop, by current attempt. */
  private static long backoff(int attempt) {
    long exp = RETRY_BASE_MS * (1L << Math.min(attempt - 1, 20));
    return Math.min(exp, RETRY_MAX_MS);
  }

  private Future<Void> commit(KafkaConsumerRecord<String, JsonObject> record) {
    String where = "topic=" + record.topic() + " partition=" + record.partition() + " offset=" + record.offset();
    Map<TopicPartition, OffsetAndMetadata> offsets = Collections.singletonMap(
        new TopicPartition(record.topic(), record.partition()),
        new OffsetAndMetadata().setOffset(record.offset() + 1));
    return consumer.commit(offsets)
        .<Void>mapEmpty()
        .onSuccess(v -> log.info("✅ Retry offset committed: {}", where))
        .onFailure(err -> log.error(
            "⚠️ Retry offset commit failed after terminal outcome; record may be redelivered (dedup skips it): {}", where, err))
        .recover(v -> Future.succeededFuture());
  }

  /**
   * In-process backoff retry used when publishing to the retry/DLQ topics
   * fails. The offset is never committed until the publish succeeds, so a
   * crash during the window simply redelivers the record after restart.
   */
  private Future<Void> retryInProcess(KafkaConsumerRecord<String, JsonObject> record, long delayMs) {
    Promise<Void> promise = Promise.promise();
    long timerId = vertx.setTimer(delayMs, id -> {
      retryTimers.remove(id);
      processRecord(record)
          .onSuccess(v -> promise.complete())
          .onFailure(err -> {
            long nextDelay = Math.min(delayMs * 2, RETRY_MAX_MS);
            retryInProcess(record, nextDelay).onComplete(promise);
          });
    });
    retryTimers.add(timerId);
    return promise.future();
  }

  /** Cancels pending timers; uncommitted offsets redeliver on restart. */
  public void stop() {
    for (Long timerId : retryTimers) {
      vertx.cancelTimer(timerId);
    }
    retryTimers.clear();
  }
}
