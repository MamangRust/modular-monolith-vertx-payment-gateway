package io.example.card.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.card.model.CardEventLog;
import io.example.card.repository.CardEventLogRepository;
import io.example.card.repository.impl.CardEventLogRepositoryImpl;
import io.example.common.chaos.ChaosManager;
import io.example.common.chaos.ChaosSqlProxy;
import io.example.common.config.AppConfig;
import io.example.common.config.TelemetryConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Event-log sink for card outbox domain events.
 *
 * Consumes card.payment.posted, card.statement.generated, card.limit.changed
 * and card.fraud.alert and appends each event to the card_event_logs table so
 * the publish-only event streams have a durable, queryable in-repo consumer
 * (audit trail).
 *
 * Like FraudScoringConsumerVerticle, it recreates its own DB pool and is
 * deployed as a worker verticle that auto-restarts on failure.
 */
public class CardEventLogVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(CardEventLogVerticle.class);

  private static final List<String> TOPICS = List.of(
      "card.payment.posted",
      "card.statement.generated",
      "card.limit.changed",
      "card.fraud.alert");

  private KafkaConsumer<String, JsonObject> consumer;
  private CardEventLogRepository eventLogRepo;
  private TelemetryConfig telemetryConfig;
  private Pool pool;

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "card-event-logger");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    telemetryConfig.initialize();

    // DB pool (own pool, independent from CardVerticle)
    AppConfig cfg = AppConfig.from(rawConfig);
    var dbCfg = cfg.getDatabaseConfig();

    PgConnectOptions connectOptions = new PgConnectOptions()
        .setHost(dbCfg.getString("host", "localhost"))
        .setPort(dbCfg.getInteger("port", 5432))
        .setDatabase(dbCfg.getString("database", "vertxdb"))
        .setUser(dbCfg.getString("user", "vertx"))
        .setPassword(dbCfg.getString("password", "vertx"))
        // PgBouncer transaction pooling drops unnamed prepared statements between
        // transactions; caching keeps statements valid per server connection.
        .setCachePreparedStatements(true);

    PoolOptions poolOptions = new PoolOptions().setMaxSize(dbCfg.getInteger("pool_size", 2));
    pool = Pool.pool(vertx, connectOptions, poolOptions);

    ChaosManager chaosManager = new ChaosManager();
    chaosManager.startWatcher(vertx);
    Pool chaosPool = ChaosSqlProxy.wrap(pool, chaosManager, vertx);
    this.eventLogRepo = new CardEventLogRepositoryImpl(chaosPool);

    // Kafka consumer
    Map<String, String> kafkaConfig = new HashMap<>();
    kafkaConfig.put("bootstrap.servers", System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092"));
    kafkaConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    kafkaConfig.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
    kafkaConfig.put("group.id", "card-event-log-group");
    kafkaConfig.put("auto.offset.reset", "earliest");

    this.consumer = KafkaConsumer.create(vertx, kafkaConfig);
    this.consumer.handler(record -> {
      JsonObject payload = record.value();
      log.debug("📥 Card event received on {}: {}", record.topic(), payload.encode());

      try {
        CardEventLog eventLog = CardEventLog.builder()
            .topic(record.topic())
            .eventType(resolveEventType(record.topic()))
            .cardNumber(payload.getString("card_number"))
            .referenceId(resolveReferenceId(record.topic(), payload))
            .payload(payload)
            .build();

        eventLogRepo.insert(eventLog)
            .onSuccess(saved -> {
              if (saved == null) {
                log.debug("⏭️ Duplicate card event skipped: {} (ref={})", record.topic(), eventLog.getReferenceId());
              } else {
                log.debug("✅ Event {} persisted: id={}", record.topic(), saved.getEventId());
              }
            })
            .onFailure(err -> log.error("❌ Failed to persist card event {}: {}", record.topic(), err.getMessage()));
      } catch (Exception e) {
        log.error("❌ Error processing card event from topic {}", record.topic(), e);
      }
    });

    this.consumer.subscribe(new HashSet<>(TOPICS))
        .onSuccess(v -> {
          log.info("✅ CardEventLogVerticle subscribed to {} topics: {}", TOPICS.size(), TOPICS);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to subscribe to card event topics", err);
          startPromise.fail(err);
        });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (consumer != null) {
      consumer.close();
    }
    if (telemetryConfig != null) {
      telemetryConfig.shutdown();
    }
    if (pool != null) {
      pool.close();
    }
    stopPromise.complete();
  }

  private static String resolveEventType(String topic) {
    return switch (topic) {
      case "card.payment.posted" -> "PAYMENT_POSTED";
      case "card.statement.generated" -> "STATEMENT_GENERATED";
      case "card.limit.changed" -> "LIMIT_CHANGED";
      case "card.fraud.alert" -> "FRAUD_ALERT";
      default -> topic.toUpperCase().replace('.', '_');
    };
  }

  private static String resolveReferenceId(String topic, JsonObject payload) {
    switch (topic) {
      case "card.payment.posted":
        return payload.getString("reference_id");
      case "card.statement.generated":
        String statementId = payload.getString("statement_id");
        return statementId != null ? statementId : payload.getString("card_number");
      case "card.fraud.alert":
        String txnId = payload.getString("txn_id");
        return txnId != null ? txnId : payload.getString("card_number");
      default:
        return payload.getString("card_number");
    }
  }
}
