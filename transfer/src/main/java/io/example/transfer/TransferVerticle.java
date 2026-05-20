package io.example.transfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.config.AppConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.common.service.KafkaService;
import io.example.common.config.KafkaConfig;
import io.example.transfer.handler.TransferCommandHandler;
import io.example.transfer.handler.TransferQueryHandler;
import io.example.transfer.handler.TransferStatsAmountHandler;
import io.example.transfer.handler.TransferStatsStatusHandler;
import io.example.transfer.repository.CardClientRepository;
import io.example.transfer.repository.SaldoClientRepository;
import io.example.transfer.repository.impl.TransferCommandRepositoryImpl;
import io.example.transfer.repository.impl.TransferQueryRepositoryImpl;
import io.example.transfer.repository.impl.TransferStatsAmountRepositoryImpl;
import io.example.transfer.repository.impl.TransferStatsByCardRepositoryImpl;
import io.example.transfer.repository.impl.TransferStatsStatusRepositoryImpl;
import io.example.transfer.service.impl.TransferCommandServiceImpl;
import io.example.transfer.service.impl.TransferQueryServiceImpl;
import io.example.transfer.service.impl.TransferStatsAmountServiceImpl;
import io.example.transfer.service.impl.TransferStatsByCardServiceImpl;
import io.example.transfer.service.impl.TransferStatsStatusServiceImpl;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import pb.card.VertxCardQueryServiceGrpcClient;
import pb.saldo.VertxSaldoCommandServiceGrpcClient;
import pb.saldo.VertxSaldoQueryServiceGrpcClient;

public class TransferVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(TransferVerticle.class);

  private TelemetryConfig telemetryConfig;
  private KafkaService kafkaService;
  private GrpcClient grpcClient;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    JsonObject config = new JsonObject()
        .put("database", new JsonObject()
            .put("host", "localhost")
            .put("port", 5432)
            .put("database", "vertxdb")
            .put("user", "vertx")
            .put("password", "vertx")
            .put("pool_size", 5))
        .put("grpc_port", 8088)
        .put("card_service_host", "localhost")
        .put("card_service_port", 8082)
        .put("saldo_service_host", "localhost")
        .put("saldo_service_port", 8084)
        .put("service.name", "transfer-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new TransferVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Transfer Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 8088");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy TransferVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Initialize Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "transfer-service");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "transfer-service");

    // 2. Initialize DB & Repositories
    AppConfig cfg = AppConfig.from(rawConfig);
    var dbCfg = cfg.getDatabaseConfig();

    PgConnectOptions connectOptions = new PgConnectOptions()
        .setHost(dbCfg.getString("host", "localhost"))
        .setPort(dbCfg.getInteger("port", 5432))
        .setDatabase(dbCfg.getString("database", "vertxdb"))
        .setUser(dbCfg.getString("user", "vertx"))
        .setPassword(dbCfg.getString("password", "vertx"));

    PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(dbCfg.getInteger("pool_size", 5));

    Pool pool = Pool.pool(vertx, connectOptions, poolOptions);

    var queryRepo = new TransferQueryRepositoryImpl(pool);
    var cmdRepo = new TransferCommandRepositoryImpl(pool);
    var statsAmountRepo = new TransferStatsAmountRepositoryImpl(pool);
    var statsStatusRepo = new TransferStatsStatusRepositoryImpl(pool);
    var statsByCardRepo = new TransferStatsByCardRepositoryImpl(pool);

    // 3. Initialize gRPC Clients
    this.grpcClient = GrpcClient.client(vertx);

    String cardHost = rawConfig.getString("card_service_host", "localhost");
    int cardPort = rawConfig.getInteger("card_service_port", 8082);
    var cardStub = new VertxCardQueryServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(cardPort, cardHost));
    var cardClientRepo = new CardClientRepository(cardStub);

    String saldoHost = rawConfig.getString("saldo_service_host", "localhost");
    int saldoPort = rawConfig.getInteger("saldo_service_port", 8084);
    var saldoQueryStub = new VertxSaldoQueryServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(saldoPort, saldoHost));
    var saldoCmdStub = new VertxSaldoCommandServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(saldoPort, saldoHost));
    var saldoClientRepo = new SaldoClientRepository(saldoQueryStub, saldoCmdStub);

    // 4. Initialize Redis & Kafka
    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    RedisService redisService = new RedisService(redisAPI, openTelemetry);
    this.kafkaService = new KafkaService(KafkaConfig.createProducer(vertx));

    // 5. Construct Service Layer
    var queryService = new TransferQueryServiceImpl(queryRepo, redisService, tracingMetrics);
    var cmdService = new TransferCommandServiceImpl(cmdRepo, cardClientRepo, saldoClientRepo, redisService,
        tracingMetrics, kafkaService);
    var statsAmountService = new TransferStatsAmountServiceImpl(statsAmountRepo, redisService, tracingMetrics);
    var statsStatusService = new TransferStatsStatusServiceImpl(statsStatusRepo, redisService, tracingMetrics);
    var statsByCardService = new TransferStatsByCardServiceImpl(statsByCardRepo, statsByCardRepo, statsByCardRepo,
        redisService, tracingMetrics);

    // 6. Construct Handler Interfaces
    var queryHandler = new TransferQueryHandler(queryService);
    var cmdHandler = new TransferCommandHandler(cmdService);
    var statsAmountHandler = new TransferStatsAmountHandler(statsAmountService, statsByCardService);
    var statsStatusHandler = new TransferStatsStatusHandler(statsStatusService, statsByCardService);

    int port = cfg.getGrpcPort();

    startGrpcServer(queryHandler, cmdHandler, statsAmountHandler, statsStatusHandler, port)
        .onSuccess(v -> {
          log.info(
              "TransferVerticle fully initialized with Decoupled CQRS and gRPC Clients. Listening for gRPC on port {}",
              port);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind Transfer gRPC server", err);
          startPromise.fail(err);
        });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (telemetryConfig != null) {
      telemetryConfig.shutdown();
    }
    if (kafkaService != null) {
      kafkaService.close();
    }
    if (grpcClient != null) {
      grpcClient.close();
    }
    stopPromise.complete();
  }

  private Future<Void> startGrpcServer(
      TransferQueryHandler queryHandler,
      TransferCommandHandler cmdHandler,
      TransferStatsAmountHandler statsAmountHandler,
      TransferStatsStatusHandler statsStatusHandler,
      int grpcPort) {
    GrpcServer grpcServer = GrpcServer.server(vertx);

    queryHandler.bindAll(grpcServer);
    cmdHandler.bindAll(grpcServer);
    statsAmountHandler.bindAll(grpcServer);
    statsStatusHandler.bindAll(grpcServer);

    return vertx.createHttpServer()
        .requestHandler(grpcServer)
        .listen(grpcPort)
        .mapEmpty();
  }
}
