package io.example.email.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.example.common.service.RedisService;
import io.example.email.observability.EmailMetrics;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Phase 6 — end-to-end integration test of {@link EmailRetryProcessor} against
 * real Kafka, real Redis, and a mock SMTP server (no mocks on the pipeline).
 *
 * <p>Covers the test matrix rows:
 * <ul>
 *   <li>SMTP sukses → email terkirim + offset di-commit;</li>
 *   <li>SMTP gagal di bawah max attempts → re-publish retry dengan attempt + 1,
 *       lalu sukses pada percobaan berikutnya;</li>
 *   <li>SMTP gagal sampai max attempts → record masuk DLQ;</li>
 *   <li>Duplicate Kafka delivery (event_id sama) → SMTP tidak mengirim dua kali;</li>
 *   <li>Invalid envelope → tidak dikirim ke SMTP, masuk DLQ.</li>
 * </ul>
 */
@Testcontainers(disabledWithoutDocker = true)
class EmailRetryProcessorIntegrationTest {

  @Container
  static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  private static Vertx vertx;
  private static MockSmtpServer smtp;
  private static EmailRetryProcessor processor;
  private static KafkaProducer<String, String> producer;
  private static KafkaConsumer<String, JsonObject> assertConsumer;
  private static KafkaConsumer<String, JsonObject> commitQueryConsumer;

  @BeforeAll
  static void setUp() throws Exception {
    vertx = Vertx.vertx();

    smtp = new MockSmtpServer(vertx);
    smtp.start();
    // keepAlive=false so every sendMail opens a fresh connection — required for
    // per-test SMTP failure injection (failNextConnections) to work in isolation.
    MailClient mailClient = MailClient.create(vertx, new MailConfig()
        .setHostname("localhost")
        .setPort(smtp.port())
        .setStarttls(StartTLSOptions.DISABLED)
        .setKeepAlive(false));

    RedisAPI redis = RedisAPI.api(Redis.createClient(vertx, new RedisOptions()
        .setType(RedisClientType.STANDALONE)
        .setConnectionString("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379))));
    EmailDedupGuard guard = new EmailDedupGuard(new RedisService(redis, OpenTelemetry.noop()));

    Map<String, String> producerConfig = new HashMap<>();
    producerConfig.put("bootstrap.servers", KAFKA.getBootstrapServers());
    producerConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    producerConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    producer = KafkaProducer.create(vertx, producerConfig);
    EmailRetryPublisher publisher = new EmailRetryPublisher(producer, EmailMetrics.noop());

    Map<String, String> consumerConfig = new HashMap<>();
    consumerConfig.put("bootstrap.servers", KAFKA.getBootstrapServers());
    consumerConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    consumerConfig.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
    consumerConfig.put("group.id", "email-service-group-retry");
    consumerConfig.put("auto.offset.reset", "earliest");
    consumerConfig.put("enable.auto.commit", "false");
    KafkaConsumer<String, JsonObject> processorConsumer = KafkaConsumer.create(vertx, consumerConfig);
    processorConsumer.subscribe(Collections.singleton(EmailRetryPublisher.RETRY_TOPIC));
    processor = new EmailRetryProcessor(vertx, processorConsumer, mailClient, guard, publisher,
        "no-reply@test.com", EmailMetrics.noop(), null);
    processor.start();

    // Reads whatever lands on the retry/DLQ topics (its own consumer group).
    Map<String, String> assertConfig = new HashMap<>(consumerConfig);
    assertConfig.put("group.id", "email-test-assert");
    assertConsumer = KafkaConsumer.create(vertx, assertConfig);
    assertConsumer.subscribe(Set.of(EmailRetryPublisher.RETRY_TOPIC, EmailRetryPublisher.DLQ_TOPIC));

    // Only used to read the committed offset of the retry group (never subscribes).
    commitQueryConsumer = KafkaConsumer.create(vertx, consumerConfig);
  }

  @AfterAll
  static void tearDown() {
    if (processor != null) processor.stop();
    if (assertConsumer != null) assertConsumer.close();
    if (commitQueryConsumer != null) commitQueryConsumer.close();
    if (producer != null) producer.close();
    if (smtp != null) smtp.stop();
    if (vertx != null) vertx.close().toCompletionStage().toCompletableFuture().join();
  }

  /* ───────── helpers ───────── */

  private static JsonObject retryPayload(int attempt, String eventId) {
    return new JsonObject()
        .put("event_id", eventId)
        .put("schema_version", 1)
        .put("event_type", "auth.register")
        .put("occurred_at", "2026-01-01T00:00:00Z")
        .put("email", "buyer@example.com")
        .put("subject", "Welcome")
        .put("body", "<p>Welcome</p>")
        .put(EmailRetryPublisher.META_ATTEMPT, attempt)
        .put(EmailRetryPublisher.META_RETRY_AT, System.currentTimeMillis() - 1_000L)
        .put(EmailRetryPublisher.META_REASON, "smtp_failure")
        .put(EmailRetryPublisher.META_SRC_TOPIC, "email-service-topic-auth-register")
        .put(EmailRetryPublisher.META_SRC_PARTITION, 0)
        .put(EmailRetryPublisher.META_SRC_OFFSET, 7L);
  }

  private static void publishRetry(JsonObject payload) {
    producer.send(KafkaProducerRecord.create(EmailRetryPublisher.RETRY_TOPIC,
            payload.getString("event_id"), payload.encode()))
        .toCompletionStage().toCompletableFuture().join();
  }

  private static void awaitTrue(CheckedSupplier<Boolean> condition, long timeoutSec, String message)
      throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSec);
    while (System.nanoTime() < deadline) {
      if (Boolean.TRUE.equals(condition.get())) {
        return;
      }
      Thread.sleep(100);
    }
    throw new AssertionError("Timed out waiting for: " + message);
  }

  private static List<JsonObject> pollAssertConsumer() throws Exception {
    io.vertx.kafka.client.consumer.KafkaConsumerRecords<String, JsonObject> batch = assertConsumer
        .poll(Duration.ofMillis(100)).toCompletionStage().toCompletableFuture().get(3, TimeUnit.SECONDS);
    List<JsonObject> payloads = new ArrayList<>();
    for (ConsumerRecord<String, JsonObject> record : batch.records()) {
      payloads.add(record.value());
    }
    return payloads;
  }

  private static JsonObject awaitRecordOn(Set<String> topics, Predicate<JsonObject> filter, long timeoutSec)
      throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSec);
    List<JsonObject> seen = new ArrayList<>();
    while (System.nanoTime() < deadline) {
      seen.addAll(pollAssertConsumer());
      for (JsonObject payload : seen) {
        if (filter.test(payload)) {
          return payload;
        }
      }
      Thread.sleep(100);
    }
    return null;
  }

  private static long committedRetryOffset() {
    OffsetAndMetadata committed = commitQueryConsumer
        .committed(new TopicPartition(EmailRetryPublisher.RETRY_TOPIC, 0))
        .toCompletionStage().toCompletableFuture().join();
    return committed == null ? -1L : committed.getOffset();
  }

  private interface CheckedSupplier<T> {
    T get() throws Exception;
  }

  /* ───────── tests ───────── */

  @Test
  void smtpSuccess_deliversAndCommits() throws Exception {
    smtp.reset();
    publishRetry(retryPayload(1, "it-ok-" + System.nanoTime()));

    awaitTrue(() -> smtp.delivered() >= 1, 15, "SMTP delivery");
    assertThat(smtp.lastMessage()).contains("Welcome");
    // Offset was committed after the send (no redelivery / no uncommitted stuck).
    awaitTrue(() -> committedRetryOffset() >= 1, 15, "retry offset commit");
    assertThat(committedRetryOffset()).isGreaterThanOrEqualTo(1);
  }

  @Test
  void smtpFailureBelowMax_republishesWithIncrementedAttempt_thenSucceeds() throws Exception {
    smtp.reset();
    smtp.failNextConnections(1);
    String eventId = "it-retry-" + System.nanoTime();
    publishRetry(retryPayload(2, eventId));

    // Escalation: attempt 2 → re-published as attempt 3 (same event id).
    JsonObject republished = awaitRecordOn(
        Collections.singleton(EmailRetryPublisher.RETRY_TOPIC),
        p -> p.getInteger(EmailRetryPublisher.META_ATTEMPT, -1) == 3, 15);
    assertThat(republished).as("retry record with _attempt=3").isNotNull();
    assertThat(republished.getString("event_id")).isEqualTo(eventId);

    // The next attempt (after backoff) succeeds against the mock → chain settles.
    awaitTrue(() -> smtp.delivered() >= 1, 20, "retry success delivery");
  }

  @Test
  void smtpFailureAtMaxAttempts_movesToDlq() throws Exception {
    smtp.reset();
    smtp.failNextConnections(1);
    publishRetry(retryPayload(EmailRetryPublisher.MAX_ATTEMPTS, "it-max-" + System.nanoTime()));

    JsonObject dlq = awaitRecordOn(
        Collections.singleton(EmailRetryPublisher.DLQ_TOPIC),
        p -> p.getInteger(EmailRetryPublisher.META_ATTEMPTS, -1) == EmailRetryPublisher.MAX_ATTEMPTS, 20);
    assertThat(dlq).as("DLQ record after max attempts").isNotNull();
    assertThat(dlq.getString(EmailRetryPublisher.META_REASON)).isEqualTo("smtp_failure");
    assertThat(smtp.delivered()).isZero();
  }

  @Test
  void duplicateEventId_deliversOnlyOnce() throws Exception {
    smtp.reset();
    String eventId = "it-dup-" + System.nanoTime();
    publishRetry(retryPayload(1, eventId));
    publishRetry(retryPayload(1, eventId));

    awaitTrue(() -> smtp.delivered() >= 1, 15, "first delivery");
    // Settle window: the duplicate must be skipped, not re-sent.
    Thread.sleep(2_000);
    assertThat(smtp.delivered()).isEqualTo(1);
  }

  @Test
  void invalidEnvelope_movesToDlqWithoutSending() throws Exception {
    smtp.reset();
    publishRetry(new JsonObject()
        .put("event_id", "it-invalid-" + System.nanoTime())
        .put("email", "buyer@example.com")); // missing schema_version/event_type/subject/body

    JsonObject dlq = awaitRecordOn(
        Collections.singleton(EmailRetryPublisher.DLQ_TOPIC),
        p -> "invalid_envelope".equals(p.getString(EmailRetryPublisher.META_REASON)), 15);
    assertThat(dlq).as("DLQ record for invalid envelope").isNotNull();
    assertThat(smtp.delivered()).isZero();
  }
}
