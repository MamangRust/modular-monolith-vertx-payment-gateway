package io.example.withdraw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.chaos.ChaosGrpcServerInterceptor;
import io.example.common.chaos.ChaosKafkaInterceptor;
import io.example.common.chaos.ChaosManager;
import io.example.common.chaos.ChaosSqlProxy;
import io.example.common.config.AppConfig;
import io.example.common.grpc.GrpcHealthService;
import io.example.common.config.RedisConfig;
import io.example.common.config.KafkaConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.withdraw.handler.WithdrawCommandHandler;
import io.example.withdraw.handler.WithdrawQueryHandler;
import io.example.withdraw.handler.WithdrawStatsAmountHandler;
import io.example.withdraw.handler.WithdrawStatsStatusHandler;
import io.example.withdraw.repository.CardClientRepository;
import io.example.withdraw.repository.SaldoClientRepository;
import io.example.withdraw.repository.WithdrawCommandRepository;
import io.example.withdraw.repository.WithdrawQueryRepository;
import io.example.withdraw.repository.WithdrawStatsAmountRepository;
import io.example.withdraw.repository.WithdrawStatsStatusRepository;
import io.example.withdraw.repository.impl.WithdrawCommandRepositoryImpl;
import io.example.withdraw.repository.impl.WithdrawQueryRepositoryImpl;
import io.example.withdraw.repository.impl.WithdrawStatsAmountRepositoryImpl;
import io.example.withdraw.repository.impl.WithdrawStatsStatusRepositoryImpl;
import io.example.withdraw.service.WithdrawCommandService;
import io.example.withdraw.service.WithdrawQueryService;
import io.example.withdraw.service.WithdrawStatsAmountService;
import io.example.withdraw.service.WithdrawStatsStatusService;
import io.example.withdraw.service.impl.WithdrawCommandServiceImpl;
import io.example.withdraw.service.impl.WithdrawQueryServiceImpl;
import io.example.withdraw.service.impl.WithdrawStatsAmountServiceImpl;
import io.example.withdraw.service.impl.WithdrawStatsStatusServiceImpl;
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
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import pb.card.VertxCardQueryServiceGrpcClient;
import pb.saldo.VertxSaldoCommandServiceGrpcClient;
import pb.saldo.VertxSaldoQueryServiceGrpcClient;

public class WithdrawVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(WithdrawVerticle.class);

  private TelemetryConfig telemetryConfig;
  private GrpcHealthService grpcHealthService;
  private KafkaService kafkaService;
  private GrpcClient grpcClient;

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
        .put("grpc_port", 50060)
        .put("card_service_host", "card")
        .put("card_service_port", 50053)
        .put("saldo_service_host", "saldo")
        .put("saldo_service_port", 50056)
        .put("service.name", "withdraw-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new WithdrawVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Withdraw Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 50060");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy WithdrawVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Initialize Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "withdraw-service");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "withdraw-service");

    // 2. Initialize DB & Repositories
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

    ChaosManager chaosManager = new ChaosManager();
    chaosManager.startWatcher(vertx);
    Pool chaosPool = ChaosSqlProxy.wrap(pool, chaosManager, vertx);

    WithdrawQueryRepository queryRepo = new WithdrawQueryRepositoryImpl(chaosPool);
    WithdrawCommandRepository cmdRepo = new WithdrawCommandRepositoryImpl(chaosPool);
    WithdrawStatsAmountRepository statsAmountRepo = new WithdrawStatsAmountRepositoryImpl(chaosPool);
    WithdrawStatsStatusRepository statsStatusRepo = new WithdrawStatsStatusRepositoryImpl(chaosPool);

    // 3. Initialize gRPC Clients
    this.grpcClient = GrpcClient.client(vertx);

    String cardHost = System.getenv().getOrDefault("CARD_SERVICE_HOST",
        rawConfig.getString("card_service_host", "card"));
    int cardPort = Integer.parseInt(
        System.getenv().getOrDefault("CARD_SERVICE_PORT",
            System.getenv().getOrDefault("GRPC_CARD_PORT",
                String.valueOf(rawConfig.getInteger("card_service_port", 50053)))));
    var cardStub = new VertxCardQueryServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(cardPort, cardHost));
    var cardClientRepo = new CardClientRepository(cardStub);

    String saldoHost = System.getenv().getOrDefault("SALDO_SERVICE_HOST",
        rawConfig.getString("saldo_service_host", "saldo"));
    int saldoPort = Integer.parseInt(
        System.getenv().getOrDefault("SALDO_SERVICE_PORT",
            System.getenv().getOrDefault("GRPC_SALDO_PORT",
                String.valueOf(rawConfig.getInteger("saldo_service_port", 50056)))));
    var saldoQueryStub = new VertxSaldoQueryServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(saldoPort, saldoHost));
    var saldoCmdStub = new VertxSaldoCommandServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(saldoPort, saldoHost));
    var saldoClientRepo = new SaldoClientRepository(saldoQueryStub, saldoCmdStub);

    // 4. Initialize Redis & Kafka
    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    RedisService redisService = new RedisService(redisAPI, openTelemetry);

    KafkaProducer<String, String> kafkaProducer = KafkaConfig.createProducer(vertx);
    KafkaProducer<String, String> chaosKafkaProducer = ChaosKafkaInterceptor.wrap(kafkaProducer, chaosManager, vertx);
    this.kafkaService = new KafkaService(chaosKafkaProducer);

    // 5. Construct Service Layer
    WithdrawQueryService queryService = new WithdrawQueryServiceImpl(queryRepo, redisService, tracingMetrics);
    WithdrawCommandService cmdService = new WithdrawCommandServiceImpl(cmdRepo, queryRepo, cardClientRepo,
        saldoClientRepo, redisService, kafkaService, tracingMetrics);
    WithdrawStatsAmountService statsAmountService = new WithdrawStatsAmountServiceImpl(statsAmountRepo, redisService,
        tracingMetrics);
    WithdrawStatsStatusService statsStatusService = new WithdrawStatsStatusServiceImpl(statsStatusRepo, redisService,
        tracingMetrics);

    // 6. Construct Handler Interfaces
    var queryHandler = new WithdrawQueryHandler(queryService);
    var cmdHandler = new WithdrawCommandHandler(cmdService);
    var statsAmountHandler = new WithdrawStatsAmountHandler(statsAmountService);
    var statsStatusHandler = new WithdrawStatsStatusHandler(statsStatusService);

    int port = cfg.getGrpcPort();

    startGrpcServer(queryHandler, cmdHandler, statsAmountHandler, statsStatusHandler, port, chaosManager)
        .onSuccess(v -> {
          log.info(
              "WithdrawVerticle fully initialized with Decoupled CQRS and gRPC Clients. Listening for gRPC on port {}",
              port);
          grpcHealthService.setServing(true);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind Withdraw gRPC server", err);
          startPromise.fail(err);
        });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (kafkaService != null) {
      kafkaService.close();
    }
    if (grpcHealthService != null) {
      grpcHealthService.setServing(false);
    }
    if (telemetryConfig != null) {
      telemetryConfig.shutdown();
    }
    if (grpcClient != null) {
      grpcClient.close();
    }
    stopPromise.complete();
  }

  private Future<Void> startGrpcServer(
      WithdrawQueryHandler queryHandler,
      WithdrawCommandHandler cmdHandler,
      WithdrawStatsAmountHandler statsAmountHandler,
      WithdrawStatsStatusHandler statsStatusHandler,
      int grpcPort,
      ChaosManager chaosManager
    ) {
    GrpcServer grpcServer = GrpcServer.server(vertx);

    queryHandler.bindAll(grpcServer);
    cmdHandler.bindAll(grpcServer);
    statsAmountHandler.bindAll(grpcServer);
    statsStatusHandler.bindAll(grpcServer);

    Handler<HttpServerRequest> chaosHandler =
        new ChaosGrpcServerInterceptor(grpcServer, chaosManager, vertx);

    grpcHealthService = new GrpcHealthService("withdraw").bind(grpcServer);
    return vertx.createHttpServer()
        .requestHandler(chaosHandler)
        .listen(grpcPort)
        .mapEmpty();
  }
}
