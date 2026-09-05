package io.example.saldo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.chaos.ChaosGrpcServerInterceptor;
import io.example.common.chaos.ChaosKafkaInterceptor;
import io.example.common.chaos.ChaosManager;
import io.example.common.chaos.ChaosSqlProxy;
import io.example.common.config.AppConfig;
import io.example.common.grpc.GrpcHealthService;
import io.example.common.config.KafkaConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.saldo.domain.requests.CreateSaldoRequest;
import io.example.saldo.handler.SaldoCommandHandler;
import io.example.saldo.handler.SaldoQueryHandler;
import io.example.saldo.handler.SaldoStatsBalanceHandler;
import io.example.saldo.handler.SaldoStatsTotalHandler;
import io.example.saldo.repository.CardClientRepository;
import io.example.saldo.repository.SaldoCommandRepository;
import io.example.saldo.repository.SaldoQueryRepository;
import io.example.saldo.repository.SaldoStatsBalanceRepository;
import io.example.saldo.repository.SaldoStatsTotalRepository;
import io.example.saldo.repository.impl.SaldoCommandRepositoryImpl;
import io.example.saldo.repository.impl.SaldoQueryRepositoryImpl;
import io.example.saldo.repository.impl.SaldoStatsBalanceRepositoryImpl;
import io.example.saldo.repository.impl.SaldoStatsTotalRepositoryImpl;
import io.example.saldo.service.SaldoCommandService;
import io.example.saldo.service.SaldoQueryService;
import io.example.saldo.service.SaldoStatsBalanceService;
import io.example.saldo.service.SaldoStatsTotalService;
import io.example.saldo.service.impl.SaldoCommandServiceImpl;
import io.example.saldo.service.impl.SaldoQueryServiceImpl;
import io.example.saldo.service.impl.SaldoStatsBalanceServiceImpl;
import io.example.saldo.service.impl.SaldoStatsTotalServiceImpl;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import pb.card.VertxCardQueryServiceGrpcClient;

import java.util.HashMap;
import java.util.Map;

public class SaldoVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(SaldoVerticle.class);

  private TelemetryConfig telemetryConfig;
  private GrpcHealthService grpcHealthService;
  private io.vertx.grpc.client.GrpcClient cardGrpcClient;
  private KafkaService kafkaService;
  private KafkaConsumer<String, JsonObject> kafkaConsumer;
  private ChaosManager chaosManager;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    JsonObject config = new JsonObject()
        .put("database", new JsonObject()
            .put("host", "postgres")
            .put("port", 5432)
            .put("database", "PAYMENT_GATEWAY")
            .put("user", "DRAGON")
            .put("password", "DRAGON")
            .put("pool_size", 5))
        .put("grpc_port", 50056)
        .put("card_service_host", "card")
        .put("card_service_port", 50053)
        .put("service.name", "saldo-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new SaldoVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Saldo Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 50056");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy SaldoVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Initialize Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "saldo-service");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "saldo-service");

    // 2. Initialize Repositories
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

    PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(dbCfg.getInteger("pool_size", 5));

    Pool pool = Pool.pool(vertx, connectOptions, poolOptions);

    this.chaosManager = new ChaosManager();
    this.chaosManager.startWatcher(vertx);
    Pool chaosPool = ChaosSqlProxy.wrap(pool, chaosManager, vertx);

    SaldoQueryRepository queryRepo = new SaldoQueryRepositoryImpl(chaosPool);
    SaldoCommandRepository cmdRepo = new SaldoCommandRepositoryImpl(chaosPool);
    SaldoStatsTotalRepository statsTotalRepo = new SaldoStatsTotalRepositoryImpl(chaosPool);
    SaldoStatsBalanceRepository statsBalRepo = new SaldoStatsBalanceRepositoryImpl(chaosPool);

    // 3. Initialize gRPC Client for Card Service
    // Env wins over config: the legacy defaults (localhost:8082) pointed nowhere in
    // Docker/K8s, where the card service is reachable at card:50053.
    String cardHost = System.getenv().getOrDefault("CARD_SERVICE_HOST",
        rawConfig.getString("card_service_host", "card"));
    int cardPort = Integer.parseInt(
        System.getenv().getOrDefault("CARD_SERVICE_PORT",
            System.getenv().getOrDefault("GRPC_CARD_PORT",
                String.valueOf(rawConfig.getInteger("card_service_port", 50053)))));
    this.cardGrpcClient = GrpcClient.client(vertx);
    var cardStub = new VertxCardQueryServiceGrpcClient(cardGrpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(cardPort, cardHost));
    var cardClientRepo = new CardClientRepository(cardStub);

    // 4. Initialize Caching
    RedisAPI redisAPI = RedisConfig.createClient(vertx);

    RedisService redisService = new RedisService(redisAPI, openTelemetry);

    // 5. Initialize Kafka (with chaos interceptor)
    this.kafkaService = new KafkaService(
        ChaosKafkaInterceptor.wrap(KafkaConfig.createProducer(vertx), chaosManager, vertx));

    // 6. Initialize Services
    SaldoQueryService queryService = new SaldoQueryServiceImpl(queryRepo, redisService,
        tracingMetrics);
    SaldoCommandService cmdService = new SaldoCommandServiceImpl(cmdRepo, queryRepo, cardClientRepo, redisService,
        kafkaService, tracingMetrics);
    SaldoStatsBalanceService statsBalService = new SaldoStatsBalanceServiceImpl(statsBalRepo, redisService,
        tracingMetrics);
    SaldoStatsTotalService statsTotalService = new SaldoStatsTotalServiceImpl(statsTotalRepo, redisService,
        tracingMetrics);

    // 7. Initialize Handlers
    var queryHandler = new SaldoQueryHandler(queryService);
    var cmdHandler = new SaldoCommandHandler(cmdService);
    var statsBalHandler = new SaldoStatsBalanceHandler(statsBalService);
    var statsTotalHandler = new SaldoStatsTotalHandler(statsTotalService);

    int port = cfg.getGrpcPort();

    startGrpcServer(queryHandler, cmdHandler, statsBalHandler, statsTotalHandler, port)
        .compose(v -> {
          log.info("SaldoVerticle fully initialized with Decoupled CQRS and gRPC Client. Listening for gRPC on port {}",
              port);

          Map<String, String> kafkaConfig = new HashMap<>();
          kafkaConfig.put("bootstrap.servers", System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092"));
          kafkaConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
          kafkaConfig.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
          kafkaConfig.put("group.id", "saldo-service-group");
          kafkaConfig.put("auto.offset.reset", "earliest");

          this.kafkaConsumer = KafkaConsumer.create(vertx, kafkaConfig);

          this.kafkaConsumer.handler(record -> {
            JsonObject payload = record.value();
            log.info("📥 Received Kafka message on topic {}: {}", record.topic(), payload.encode());

            try {
              String cardNumber = payload.getString("card_number");
              Long totalBalance = 0L;
              if (payload.containsKey("total_balance")) {
                Number balNum = payload.getNumber("total_balance");
                if (balNum != null) {
                  totalBalance = balNum.longValue();
                }
              }

              if (cardNumber == null) {
                log.warn("⚠️ Received incomplete saldo payload: {}", payload.encode());
                return;
              }

              CreateSaldoRequest createReq = CreateSaldoRequest.builder()
                  .cardNumber(cardNumber)
                  .totalBalance(totalBalance)
                  .build();

              cmdService.createSaldo(createReq)
                  .onSuccess(saldoResponse -> log.info("✅ Successfully handled create saldo from Kafka for card: {}",
                      cardNumber))
                  .onFailure(
                      err -> log.error("❌ Exception handling create saldo from Kafka for card: {}", cardNumber, err));

            } catch (Exception e) {
              log.error("❌ Error parsing/processing Kafka message from topic {}", record.topic(), e);
            }
          });

          return this.kafkaConsumer.subscribe("saldo-service-topic-create-saldo")
              .onSuccess(
                  x -> log.info("📡 Saldo Service successfully subscribed to topic: saldo-service-topic-create-saldo"))
              .mapEmpty();
        })
        .onSuccess(v -> {
          grpcHealthService.setServing(true);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("❌ Failed to start SaldoVerticle", err);
          startPromise.fail(err);
        });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (grpcHealthService != null) {
      grpcHealthService.setServing(false);
    }
    if (telemetryConfig != null) {
      telemetryConfig.shutdown();
    }
    if (cardGrpcClient != null) {
      cardGrpcClient.close();
    }
    if (kafkaService != null) {
      kafkaService.close();
    }
    if (kafkaConsumer != null) {
      kafkaConsumer.close();
    }
    stopPromise.complete();
  }

  private Future<Void> startGrpcServer(
      SaldoQueryHandler query,
      SaldoCommandHandler cmd,
      SaldoStatsBalanceHandler statsBal,
      SaldoStatsTotalHandler statsTotal,
      int grpcPort) {
    GrpcServer grpcServer = GrpcServer.server(vertx);

    query.bindAll(grpcServer);
    cmd.bindAll(grpcServer);
    statsBal.bindAll(grpcServer);
    statsTotal.bindAll(grpcServer);

    Handler<HttpServerRequest> chaosHandler =
        new ChaosGrpcServerInterceptor(grpcServer, chaosManager, vertx);

    grpcHealthService = new GrpcHealthService("saldo").bind(grpcServer);
    return vertx.createHttpServer()
        .requestHandler(chaosHandler)
        .listen(grpcPort)
        .mapEmpty();
  }
}
