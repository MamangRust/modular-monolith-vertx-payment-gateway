package io.example.email;

import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.event.EventEnvelope;
import io.example.common.service.RedisService;
import io.example.email.config.KafkaSecurityConfig;
import io.example.email.observability.ConsumerLagMonitor;
import io.example.email.observability.EmailMetrics;
import io.example.email.observability.EmailTracing;
import io.example.email.observability.SmtpHealthCheck;
import io.example.email.service.EmailDedupGuard;
import io.example.email.service.EmailRetryProcessor;
import io.example.email.service.EmailRetryPublisher;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.redis.client.RedisAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class EmailVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(EmailVerticle.class);

  /** Backoff floor/cap for the in-process fallback when publishing to retry/DLQ fails. */
  private static final long RETRY_BASE_MS = 2_000;
  private static final long RETRY_MAX_MS = 300_000;
  private static final long DEFAULT_SMTP_HEALTH_TIMEOUT_MS = 3_000;
  private static final long DEFAULT_LAG_POLL_MS = 15_000;

  private MailClient mailClient;
  private KafkaConsumer<String, JsonObject> consumer;
  private KafkaConsumer<String, JsonObject> retryConsumer;
  private TelemetryConfig telemetryConfig;
  private RedisService redisService;
  private EmailDedupGuard dedupGuard;
  private EmailRetryPublisher retryPublisher;
  private EmailRetryProcessor retryProcessor;
  private EmailMetrics emailMetrics;
  private EmailTracing emailTracing;
  private ConsumerLagMonitor lagMonitor;
  private SmtpHealthCheck smtpHealthCheck;
  private boolean smtpHealthEnabled;
  private HttpServer healthServer;
  private volatile boolean ready;
  private long subscriptionRetryTimer = -1;

  // Serialized processing chain: records are handled one at a time so an offset
  // is only committed after the email was actually delivered (or the record was
  // proven duplicate/invalid, or handed to the retry topic). A record whose
  // retry/DLQ publish fails keeps its offset uncommitted and is retried
  // in-process; on crash/restart it is redelivered (at-least-once).
  private Future<Void> processingChain = Future.succeededFuture();
  private final Set<Long> retryTimers = new HashSet<>();

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject config = config();

    // 1. Setup Telemetry + email metrics/tracing
    telemetryConfig = new TelemetryConfig(config);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    emailMetrics = new EmailMetrics(openTelemetry);
    emailTracing = new EmailTracing(openTelemetry);

    // 2. Setup Redis (for email deduplication / idempotency)
    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    redisService = new RedisService(redisAPI, openTelemetry);
    dedupGuard = new EmailDedupGuard(redisService, emailMetrics);

    // 3. Setup Mail Client
    String smtpHost = System.getenv().getOrDefault("SMTP_SERVER", "localhost");
    int smtpPort = Integer.parseInt(System.getenv().getOrDefault("SMTP_PORT", "587"));
    MailConfig mailConfig = new MailConfig()
        .setHostname(smtpHost)
        .setPort(smtpPort)
        .setUsername(System.getenv("SMTP_USER"))
        .setPassword(System.getenv("SMTP_PASS"))
        .setStarttls(io.vertx.ext.mail.StartTLSOptions.REQUIRED);

    mailClient = MailClient.createShared(vertx, mailConfig);

    // 4. SMTP health check (readiness: smtp-connectivity round-trip)
    smtpHealthEnabled = Boolean.parseBoolean(System.getenv().getOrDefault("smtp.health.enabled", "true"));
    long smtpHealthTimeout = Long.parseLong(System.getenv()
        .getOrDefault("smtp.health.timeout-ms", String.valueOf(DEFAULT_SMTP_HEALTH_TIMEOUT_MS)));
    smtpHealthCheck = new SmtpHealthCheck(vertx, smtpHost, smtpPort, smtpHealthTimeout);

    // 5. Setup retry/DLQ publisher and the retry worker consumer. The retry
    // worker consumes the unified retry topic and escalates attempts until the
    // record is delivered or moved to the DLQ (Phase 4).
    Map<String, String> producerConfig = new HashMap<>();
    producerConfig.put("bootstrap.servers", System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092"));
    producerConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    producerConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    KafkaSecurityConfig.apply(producerConfig);
    retryPublisher = new EmailRetryPublisher(KafkaProducer.create(vertx, producerConfig), emailMetrics, emailTracing);

    // 6. Setup Kafka Consumer
    Map<String, String> kafkaConfig = new HashMap<>();
    kafkaConfig.put("bootstrap.servers", System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092"));
    kafkaConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    kafkaConfig.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
    kafkaConfig.put("group.id", "email-service-group");
    kafkaConfig.put("auto.offset.reset", "earliest");
    // Never auto-commit: offsets must only advance after the email was actually
    // delivered (or the record proven duplicate/invalid). Otherwise a crash or
    // SMTP failure would permanently lose the email.
    kafkaConfig.put("enable.auto.commit", "false");
    KafkaSecurityConfig.apply(kafkaConfig);

    consumer = KafkaConsumer.create(vertx, kafkaConfig);

    // Retry worker uses its own consumer group so retry-topic processing is
    // independent of the main consumer's offset commits.
    Map<String, String> retryKafkaConfig = new HashMap<>(kafkaConfig);
    retryKafkaConfig.put("group.id", "email-service-group-retry");
    retryConsumer = KafkaConsumer.create(vertx, retryKafkaConfig);
    retryProcessor = new EmailRetryProcessor(vertx, retryConsumer, mailClient, dedupGuard, retryPublisher,
        System.getenv().getOrDefault("SMTP_FROM", "no-reply@payment-gateway.com"), emailMetrics, emailTracing);
    retryProcessor.start();

    long lagPollMs = Long.parseLong(System.getenv().getOrDefault("EMAIL_LAG_POLL_MS",
        String.valueOf(DEFAULT_LAG_POLL_MS)));
    lagMonitor = new ConsumerLagMonitor(vertx, consumer, emailMetrics, lagPollMs);

    // List of topics to subscribe to
    List<String> topics = Arrays.asList(
        "email-service-topic-auth-register",
        "email-service-topic-auth-forgot-password",
        "email-service-topic-auth-verify-code-success",
        "email-service-topic-saldo-create",
        "email-service-topic-topup-create",
        "email-service-topic-transaction-create",
        "email-service-topic-transfer-create",
        "email-service-topic-withdraw-create",
        "email-service-topic-withdraw-update",
        "email-service-topic-merchant-create",
        "email-service-topic-merchant-update-status",
        "email-service-topic-merchant-document-create",
        "email-service-topic-merchant-document-update-status"
    );

    consumer.handler(record -> {
      processingChain = processingChain.compose(v -> processRecord(record));
    });

    int healthPort = Integer.parseInt(System.getenv().getOrDefault("METRIC_EMAIL_ADDR", "8080"));
    healthServer = vertx.createHttpServer().requestHandler(request -> {
      String path = request.path();
      if ("/health/live".equals(path)) {
        request.response().setStatusCode(200).end("{\"status\":\"alive\"}");
      } else if ("/health/ready".equals(path)) {
        handleReady(request);
      } else {
        request.response().setStatusCode(404).end();
      }
    });

    healthServer.listen(healthPort).onSuccess(ignored -> {
      // The process is alive even while Kafka is unavailable; readiness remains 503
      // until subscription succeeds and the retry loop can recover transient outages.
      startPromise.complete();
      subscribeWithRetry(topics);
    }).onFailure(startPromise::fail);
  }

  private void subscribeWithRetry(List<String> topics) {
    Future.all(
            consumer.subscribe(new HashSet<>(topics)),
            retryConsumer.subscribe(Collections.singleton(EmailRetryPublisher.RETRY_TOPIC)))
        .onSuccess(v -> {
          ready = true;
          lagMonitor.start();
          log.info("📧 Email Service successfully started and subscribed to {} topics + retry topic", topics.size());
        })
        .onFailure(err -> {
          ready = false;
          log.warn("Kafka subscription unavailable; retrying in 5 seconds", err);
          subscriptionRetryTimer = vertx.setTimer(5000, ignored -> subscribeWithRetry(topics));
        });
  }

  private void handleReady(io.vertx.core.http.HttpServerRequest request) {
    if (!ready) {
      request.response().setStatusCode(503).end("{\"status\":\"not_ready\"}");
      return;
    }
    if (!smtpHealthEnabled) {
      request.response().setStatusCode(200).end("{\"status\":\"ready\"}");
      return;
    }
    smtpHealthCheck.check().onComplete(ar -> {
      if (Boolean.TRUE.equals(ar.result())) {
        request.response().setStatusCode(200).end("{\"status\":\"ready\",\"smtp\":\"up\"}");
      } else {
        log.warn("🚫 SMTP health check failed: {}", ar.cause() == null ? "connectivity" : ar.cause().getMessage());
        request.response().setStatusCode(503).end("{\"status\":\"not_ready\",\"smtp\":\"down\"}");
      }
    });
  }

  private Future<Void> processRecord(KafkaConsumerRecord<String, JsonObject> record) {
    Span span = emailTracing.startConsumeSpan(record, "email.consume");
    Context otelContext = span.storeInContext(Context.current());
    long startNanos = System.nanoTime();
    Future<Void> result;
    try (Scope ignored = otelContext.makeCurrent()) {
      result = doProcessRecord(record, otelContext);
    }
    return result.onComplete(v -> {
      emailTracing.endSpan(span, v.succeeded(), v.failed() ? v.cause().getMessage() : "processed");
      emailMetrics.recordProcessingDuration((System.nanoTime() - startNanos) / 1_000_000_000.0);
    });
  }

  private Future<Void> doProcessRecord(KafkaConsumerRecord<String, JsonObject> record, Context otelContext) {
    String where = "topic=" + record.topic() + " partition=" + record.partition() + " offset=" + record.offset();
    try {
      JsonObject payload = record.value();

      if (!EventEnvelope.isValid(payload)) {
        // Permanently-invalid payloads go to the DLQ (not SMTP). Commit only
        // after the DLQ publish succeeds, so nothing is silently dropped.
        log.error("⚠️ Invalid event envelope, moving to DLQ: {}", where);
        emailMetrics.recordInvalid();
        return inContext(otelContext, () -> retryPublisher.publishDlq(payload, payload.getString("event_id"),
                1, "invalid_envelope", record.topic(), record.partition(), record.offset()))
            .compose(v -> commit(record))
            .recover(err -> {
              log.error("⚠️ Failed to publish to DLQ, offset NOT committed: {}", where, err);
              return retryRecord(record, RETRY_BASE_MS);
            });
      }

      String eventId = payload.getString("event_id");
      return dedupGuard.claim(eventId, record.topic(), record.partition(), record.offset())
          .compose(result -> {
            switch (result) {
              case DUPLICATE:
                // Already delivered by a previous attempt — skip, commit.
                log.info("⏭️ Duplicate record (already SENT), committing without sending: {}", where);
                emailMetrics.recordDuplicate();
                return commit(record);
              case BUSY:
                // Claim held by another worker/lease; defer via the retry topic
                // so the source offset can advance without losing the event.
                log.info("⏳ Claim busy (lease held elsewhere), deferring via retry topic: {}", where);
                return inContext(otelContext, () -> retryPublisher.publishRetry(payload, eventId, 1,
                        System.currentTimeMillis() + RETRY_BASE_MS, "busy",
                        record.topic(), record.partition(), record.offset()))
                    .compose(v -> commit(record))
                    .recover(err -> {
                      log.error("⚠️ Failed to defer busy record to retry topic, offset NOT committed: {}", where, err);
                      return retryRecord(record, RETRY_BASE_MS);
                    });
              case CLAIMED:
              default:
                return sendEmail(payload, otelContext)
                    .compose(v -> dedupGuard.markSent(eventId, record.topic(), record.partition(), record.offset()))
                    .compose(v -> commit(record))
                    .recover(err -> {
                      // Only an SMTP failure reaches here (markSent/commit failures
                      // are handled inside those calls). Release the claim and hand
                      // the record to the retry topic (attempt 1); commit the
                      // source offset only after the retry publish succeeds, since
                      // the retry topic now owns the record.
                      log.error("❌ SMTP send failed, moving to retry topic (offset committed after publish): {}", where, err);
                      emailMetrics.recordFailed(payload.getString("event_type"), "smtp");
                      return dedupGuard.release(eventId, record.topic(), record.partition(), record.offset())
                          .compose(v -> inContext(otelContext, () -> retryPublisher.publishRetry(payload, eventId, 1,
                              System.currentTimeMillis() + RETRY_BASE_MS, "smtp_failure",
                              record.topic(), record.partition(), record.offset())))
                          .compose(v -> commit(record))
                          .recover(publishErr -> {
                            log.error("⚠️ Failed to publish retry, offset NOT committed: {}", where, publishErr);
                            return retryRecord(record, RETRY_BASE_MS);
                          });
                    });
            }
          });
    } catch (Exception e) {
      // Never let a synchronous error break the chain: keep the offset uncommitted
      // and retry so the record is eventually delivered or redelivered on restart.
      log.error("❌ Unexpected error processing record, retrying with backoff (offset NOT committed): {}", where, e);
      emailMetrics.recordFailed("unknown", "unexpected");
      return retryRecord(record, RETRY_BASE_MS);
    }
  }

  private Future<Void> sendEmail(JsonObject payload, Context otelContext) {
    MailMessage message = new MailMessage()
        .setFrom(System.getenv().getOrDefault("SMTP_FROM", "no-reply@payment-gateway.com"))
        .setTo(payload.getString("email"))
        .setSubject(payload.getString("subject"))
        .setHtml(payload.getString("body")); // Using setHtml as most notification bodies are HTML

    Span span = startSmtpSpan(otelContext);
    return mailClient.sendMail(message)
        .<Void>mapEmpty()
        .onComplete(ar -> {
          emailTracing.endSpan(span, ar.succeeded(), ar.failed() ? ar.cause().getMessage() : "sent");
          if (ar.succeeded()) {
            emailMetrics.recordSent(payload.getString("event_type"));
            log.info("✅ Email successfully sent to {}", payload.getString("email"));
          }
        });
  }

  private Span startSmtpSpan(Context otelContext) {
    try (Scope ignored = otelContext.makeCurrent()) {
      return emailTracing.startSpan("email.smtp.send", SpanKind.CLIENT);
    }
  }

  /** Runs {@code action} with the consume context current (parent for spans/injection). */
  private <T> Future<T> inContext(Context otelContext, Supplier<Future<T>> action) {
    try (Scope ignored = otelContext.makeCurrent()) {
      return action.get();
    }
  }

  /**
   * Commits exactly this record's offset (offset + 1). Only called after the
   * email was delivered or the record was proven duplicate/invalid.
   */
  private Future<Void> commit(KafkaConsumerRecord<String, JsonObject> record) {
    String where = "topic=" + record.topic() + " partition=" + record.partition() + " offset=" + record.offset();
    Map<TopicPartition, OffsetAndMetadata> offsets = Collections.singletonMap(
        new TopicPartition(record.topic(), record.partition()),
        new OffsetAndMetadata().setOffset(record.offset() + 1));
    return consumer.commit(offsets)
        .<Void>mapEmpty()
        .onSuccess(v -> log.info("✅ Offset committed: {}", where))
        .onFailure(err -> log.error(
            "⚠️ Offset commit failed after delivery; record may be redelivered (duplicate possible, at-least-once): {}", where, err))
        .recover(v -> Future.succeededFuture());
  }

  /**
   * In-process fallback with capped exponential backoff, used only when
   * publishing to the retry/DLQ topics fails (SMTP failures go to the retry
   * topic instead). The offset is never committed until the publish succeeds,
   * so a crash during the retry window simply redelivers the record after
   * restart (at-least-once).
   */
  private Future<Void> retryRecord(KafkaConsumerRecord<String, JsonObject> record, long delayMs) {
    Promise<Void> promise = Promise.promise();
    long timerId = vertx.setTimer(delayMs, id -> {
      retryTimers.remove(id);
      processRecord(record)
          .onSuccess(v -> promise.complete())
          .onFailure(err -> {
            long nextDelay = Math.min(delayMs * 2, RETRY_MAX_MS);
            retryRecord(record, nextDelay).onComplete(promise);
          });
    });
    retryTimers.add(timerId);
    return promise.future();
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    ready = false;
    if (subscriptionRetryTimer != -1) {
      vertx.cancelTimer(subscriptionRetryTimer);
      subscriptionRetryTimer = -1;
    }
    // Abandon pending retries: offsets stay uncommitted, so the records are
    // redelivered when the service restarts.
    for (Long timerId : retryTimers) {
      vertx.cancelTimer(timerId);
    }
    retryTimers.clear();
    if (lagMonitor != null) lagMonitor.stop();
    if (retryProcessor != null) retryProcessor.stop();
    if (consumer != null) consumer.close();
    if (retryConsumer != null) retryConsumer.close();
    if (retryPublisher != null) retryPublisher.close();
    Future<Void> closeHealth = healthServer != null ? healthServer.close() : Future.succeededFuture();
    closeHealth.onComplete(ignored -> {
      if (telemetryConfig != null) telemetryConfig.shutdown();
      stopPromise.complete();
    });
  }
}
