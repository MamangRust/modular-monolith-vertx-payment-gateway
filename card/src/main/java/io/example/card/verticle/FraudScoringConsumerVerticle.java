package io.example.card.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.card.repository.CardAuthTransactionRepository;
import io.example.card.repository.CardCreditAccountRepository;
import io.example.card.repository.impl.CardAuthTransactionRepositoryImpl;
import io.example.card.repository.impl.CardCreditAccountRepositoryImpl;
import io.example.common.chaos.ChaosKafkaInterceptor;
import io.example.common.chaos.ChaosManager;
import io.example.common.chaos.ChaosSqlProxy;
import io.example.common.config.AppConfig;
import io.example.common.config.KafkaConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Fraud scoring consumer that listens on card.txn.created and applies
 * rule-based risk scoring. If score exceeds threshold, blocks the card.
 *
 * On startup, this verticle recreates its own DB pool and Redis client
 * independently from CardVerticle — it can be deployed as a Worker Verticle
 * and will auto-restart on failure.
 */
public class FraudScoringConsumerVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(FraudScoringConsumerVerticle.class);

  private KafkaConsumer<String, JsonObject> consumer;
  private KafkaService kafkaService;
  private TelemetryConfig telemetryConfig;
  private Pool pool;

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "card-fraud-scorer");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "card-fraud-scorer");

    // DB pool
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
    Pool chaosPool = ChaosSqlProxy.wrap(pool, chaosManager, vertx);

    CardAuthTransactionRepository authTxnRepo = new CardAuthTransactionRepositoryImpl(chaosPool);
    CardCreditAccountRepository creditRepo = new CardCreditAccountRepositoryImpl(chaosPool);

    // Kafka producer for publishing fraud alerts (same wiring as BillingSchedulerVerticle)
    KafkaProducer<String, String> kafkaProducer = KafkaConfig.createProducer(vertx);
    this.kafkaService = new KafkaService(
        ChaosKafkaInterceptor.wrap(kafkaProducer, chaosManager, vertx));

    // Kafka consumer config
    Map<String, String> kafkaConfig = new HashMap<>();
    kafkaConfig.put("bootstrap.servers", System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092"));
    kafkaConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    kafkaConfig.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
    kafkaConfig.put("group.id", "card-fraud-scorer-group");
    kafkaConfig.put("auto.offset.reset", "earliest");

    this.consumer = KafkaConsumer.create(vertx, kafkaConfig);

    this.consumer.handler(record -> {
      JsonObject payload = record.value();
      log.info("🔍 FraudScoring evaluating txn: {}", payload.encode());

      try {
        String txnId = payload.getString("txn_id");
        String cardNumber = payload.getString("card_number");
        Long amount = payload.getLong("amount", 0L);
        String mcc = payload.getString("mcc", "");
        String status = payload.getString("status", "");

        if (txnId == null || cardNumber == null) {
          log.warn("Incomplete txn payload: {}", payload.encode());
          return;
        }

        // Rule-based risk scoring
        int riskScore = computeRiskScore(amount, mcc);

        // Update risk score on the transaction
        authTxnRepo.updateRiskScore(txnId, riskScore);

        if (riskScore >= 70) {
          // High risk — block card. Threshold is >= 70 (not > 70): the scoring
          // engine's max achievable score is 70 (high amount 30 + gambling 40),
          // so a strict > would make the block branch unreachable dead code.
          // E.g. amount 15M + MCC 7995 → 70 → BLOCKED.
          log.warn("🚨 HIGH RISK ({}): blocking card {}", riskScore, cardNumber);
          creditRepo.updateStatus(cardNumber, "BLOCKED");
          // Publish fraud alert
          JsonObject alert = new JsonObject()
              .put("txn_id", txnId)
              .put("card_number", cardNumber)
              .put("risk_score", riskScore)
              .put("reason", "High risk score: " + riskScore)
              .put("timestamp", java.time.Instant.now().toString());
          if (kafkaService != null) {
            kafkaService.sendMessage("card.fraud.alert", cardNumber, alert);
          }
        } else if (riskScore >= 30) {
          // Medium risk — flag for review
          log.info("⚠️ MEDIUM RISK ({}): flagging txn {} for review", riskScore, txnId);
          JsonObject flag = new JsonObject()
              .put("txn_id", txnId)
              .put("card_number", cardNumber)
              .put("risk_score", riskScore)
              .put("action", "REVIEW")
              .put("timestamp", java.time.Instant.now().toString());
          if (kafkaService != null) {
            kafkaService.sendMessage("card.fraud.alert", cardNumber, flag);
          }
        } else {
          log.debug("✅ LOW RISK ({}): txn {} passed", riskScore, txnId);
        }

      } catch (Exception e) {
        log.error("Error processing fraud scoring for message: {}", record.value(), e);
      }
    });

    this.consumer.subscribe("card.txn.created")
        .onSuccess(v -> {
          log.info("✅ FraudScoringConsumer subscribed to card.txn.created");
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to subscribe to card.txn.created", err);
          startPromise.fail(err);
        });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (consumer != null) {
      consumer.close();
    }
    if (kafkaService != null) {
      kafkaService.close();
    }
    if (telemetryConfig != null) {
      telemetryConfig.shutdown();
    }
    if (pool != null) {
      pool.close();
    }
    stopPromise.complete();
  }

  /**
   * Rule-based risk scoring engine.
   * Returns score 0-100 based on transaction characteristics.
   */
  public static int computeRiskScore(Long amount, String mcc) {
    int score = 0;

    // High-value transaction risk
    if (amount > 10_000_000L) { // > 100,000 IDR
      score += 30;
    } else if (amount > 5_000_000L) { // > 50,000 IDR
      score += 15;
    } else if (amount > 1_000_000L) { // > 10,000 IDR
      score += 5;
    }

    // High-risk MCC categories
    if (mcc != null) {
      switch (mcc) {
        case "7995": // Gambling
          score += 40;
          break;
        case "6051": // Crypto / Non-financial institutions
          score += 35;
          break;
        case "4829": // Money transfer
          score += 20;
          break;
        case "5813": // Bars
        case "5933": // Pawn shops
          score += 15;
          break;
        default:
          break;
      }
    }

    return Math.min(100, score);
  }
}
