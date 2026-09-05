package io.example.card.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.card.repository.BillingStatementRepository;
import io.example.card.repository.CardCreditAccountRepository;
import io.example.card.repository.impl.BillingStatementRepositoryImpl;
import io.example.card.repository.impl.CardCreditAccountRepositoryImpl;
import io.example.card.service.BillingEngineService;
import io.example.card.service.impl.BillingEngineServiceImpl;
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
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

import java.time.LocalDate;

/**
 * Billing scheduler that triggers monthly billing cycles.
 *
 * Timer mode: uses vertx.setPeriodic to trigger billing at the configured time.
 * On-demand billing is served synchronously through the gRPC CardBillingService
 * (gateway route POST /api/v1/cards/billing/trigger) — NOT via Kafka, so there
 * is no orphan consumer on card.billing.trigger.
 */
public class BillingSchedulerVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(BillingSchedulerVerticle.class);

  private BillingEngineService billingEngine;
  private TelemetryConfig telemetryConfig;
  private Pool pool;
  private KafkaService kafkaService;
  private long timerId = -1;

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "card-billing-scheduler");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "card-billing-scheduler");

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

    CardCreditAccountRepository creditRepo = new CardCreditAccountRepositoryImpl(chaosPool);
    BillingStatementRepository stmtRepo = new BillingStatementRepositoryImpl(chaosPool);

    // Kafka producer for publishing events
    var kafkaProducer = KafkaConfig.createProducer(vertx);
    this.kafkaService = new KafkaService(
        io.example.common.chaos.ChaosKafkaInterceptor.wrap(kafkaProducer, chaosManager, vertx));

    this.billingEngine = new BillingEngineServiceImpl(creditRepo, stmtRepo, tracingMetrics, kafkaService);

    int billingHour = rawConfig.getInteger("billing_hour", 2); // 2 AM default
    int billingMinute = rawConfig.getInteger("billing_minute", 0);

    // Schedule periodic billing check: run every hour, check if any cycle day matches today
    long intervalMs = 3600_000L; // 1 hour
    this.timerId = vertx.setPeriodic(intervalMs, id -> {
      int todayDayOfMonth = LocalDate.now().getDayOfMonth();
      log.info("⏰ Billing scheduler tick: checking cycle day {}", todayDayOfMonth);

      billingEngine.triggerBillingCycle(todayDayOfMonth)
          .onSuccess(count -> {
            if (count > 0) {
              log.info("✅ Billing cycle completed: {} statements generated for cycle day {}",
                  count, todayDayOfMonth);
            }
          })
          .onFailure(err -> log.error("❌ Billing cycle failed for day {}: {}", todayDayOfMonth, err.getMessage()));
    });

    log.info("✅ BillingScheduler started (periodic check every {}ms, billing hour {}:{})",
        intervalMs, billingHour, billingMinute);
    startPromise.complete();
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (timerId >= 0) {
      vertx.cancelTimer(timerId);
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
}
