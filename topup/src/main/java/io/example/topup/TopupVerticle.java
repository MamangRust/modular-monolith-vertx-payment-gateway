package io.example.topup;

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
import io.example.topup.handler.TopupCommandHandler;
import io.example.topup.handler.TopupQueryHandler;
import io.example.topup.handler.TopupStatsAmountHandler;
import io.example.topup.handler.TopupStatsMethodHandler;
import io.example.topup.handler.TopupStatsStatusHandler;
import io.example.topup.repository.CardClientRepository;
import io.example.topup.repository.SaldoClientRepository;
import io.example.topup.repository.impl.TopupCommandRepositoryImpl;
import io.example.topup.repository.impl.TopupQueryRepositoryImpl;
import io.example.topup.repository.impl.TopupStatsAmountRepositoryImpl;
import io.example.topup.repository.impl.TopupStatsByCardAmountRepositoryImpl;
import io.example.topup.repository.impl.TopupStatsByCardMethodRepositoryImpl;
import io.example.topup.repository.impl.TopupStatsByCardStatusRepositoryImpl;
import io.example.topup.repository.impl.TopupStatsMethodRepositoryImpl;
import io.example.topup.repository.impl.TopupStatsStatusRepositoryImpl;
import io.example.topup.service.impl.TopupCommandServiceImpl;
import io.example.topup.service.impl.TopupQueryServiceImpl;
import io.example.topup.service.impl.TopupStatsAmountServiceImpl;
import io.example.topup.service.impl.TopupStatsMethodServiceImpl;
import io.example.topup.service.impl.TopupStatsStatusServiceImpl;
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
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import pb.card.VertxCardCommandServiceGrpcClient;
import pb.card.VertxCardQueryServiceGrpcClient;
import pb.saldo.VertxSaldoCommandServiceGrpcClient;
import pb.saldo.VertxSaldoQueryServiceGrpcClient;

public class TopupVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(TopupVerticle.class);

  private TelemetryConfig telemetryConfig;
  private GrpcHealthService grpcHealthService;
  private KafkaService kafkaService;
  private GrpcClient grpcClient;
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
        .put("grpc_port", 50057)
        .put("card_service_host", "card")
        .put("card_service_port", 50053)
        .put("saldo_service_host", "saldo")
        .put("saldo_service_port", 50056)
        .put("service.name", "topup-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new TopupVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Topup Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 50057");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy TopupVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Initialize Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "topup-service");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "topup-service");

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

    var queryRepo = new TopupQueryRepositoryImpl(chaosPool);
    var cmdRepo = new TopupCommandRepositoryImpl(chaosPool);
    var statsAmountRepo = new TopupStatsAmountRepositoryImpl(chaosPool);
    var statsAmountCardRepo = new TopupStatsByCardAmountRepositoryImpl(chaosPool);
    var statsMethodRepo = new TopupStatsMethodRepositoryImpl(chaosPool);
    var statsMethodCardRepo = new TopupStatsByCardMethodRepositoryImpl(chaosPool);
    var statsStatusRepo = new TopupStatsStatusRepositoryImpl(chaosPool);
    var statsStatusCardRepo = new TopupStatsByCardStatusRepositoryImpl(chaosPool);

    // 3. Initialize gRPC Clients
    this.grpcClient = GrpcClient.client(vertx);

    String cardHost = System.getenv().getOrDefault("CARD_SERVICE_HOST",
        rawConfig.getString("card_service_host", "card"));
    int cardPort = Integer.parseInt(
        System.getenv().getOrDefault("CARD_SERVICE_PORT",
            System.getenv().getOrDefault("GRPC_CARD_PORT",
                String.valueOf(rawConfig.getInteger("card_service_port", 50053)))));
    var cardQueryStub = new VertxCardQueryServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(cardPort, cardHost));
    var cardCmdStub = new VertxCardCommandServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(cardPort, cardHost));
    var cardClientRepo = new CardClientRepository(cardQueryStub, cardCmdStub);

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

    // 4. Initialize Caching & Messaging
    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    RedisService redisService = new RedisService(redisAPI, openTelemetry);
    this.kafkaService = new KafkaService(
        ChaosKafkaInterceptor.wrap(KafkaConfig.createProducer(vertx), chaosManager, vertx));

    // 5. Initialize Services
    var queryService = new TopupQueryServiceImpl(queryRepo, redisService, tracingMetrics);
    var cmdService = new TopupCommandServiceImpl(cmdRepo, queryRepo, cardClientRepo, saldoClientRepo, redisService,
        tracingMetrics, kafkaService);
    var statsAmountService = new TopupStatsAmountServiceImpl(statsAmountRepo, statsAmountCardRepo, redisService,
        tracingMetrics);
    var statsMethodService = new TopupStatsMethodServiceImpl(statsMethodRepo, statsMethodCardRepo, redisService,
        tracingMetrics);
    var statsStatusService = new TopupStatsStatusServiceImpl(statsStatusRepo, statsStatusCardRepo, redisService,
        tracingMetrics);

    // 6. Initialize Handlers
    var queryHandler = new TopupQueryHandler(queryService);
    var cmdHandler = new TopupCommandHandler(cmdService);
    var statsAmountHandler = new TopupStatsAmountHandler(statsAmountService);
    var statsMethodHandler = new TopupStatsMethodHandler(statsMethodService);
    var statsStatusHandler = new TopupStatsStatusHandler(statsStatusService);

    int port = cfg.getGrpcPort();

    startGrpcServer(queryHandler, cmdHandler, statsAmountHandler, statsMethodHandler, statsStatusHandler, port)
        .onSuccess(v -> {
          log.info(
              "TopupVerticle fully initialized with Decoupled CQRS and gRPC Clients. Listening for gRPC on port {}",
              port);
          grpcHealthService.setServing(true);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind Topup gRPC server", err);
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
    if (kafkaService != null) {
      kafkaService.close();
    }
    if (grpcClient != null) {
      grpcClient.close();
    }
    stopPromise.complete();
  }

  private Future<Void> startGrpcServer(
      TopupQueryHandler queryHandler,
      TopupCommandHandler cmdHandler,
      TopupStatsAmountHandler statsAmountHandler,
      TopupStatsMethodHandler statsMethodHandler,
      TopupStatsStatusHandler statsStatusHandler,
      int grpcPort) {
    GrpcServer grpcServer = GrpcServer.server(vertx);

    queryHandler.bindAll(grpcServer);
    cmdHandler.bindAll(grpcServer);

    statsAmountHandler.bindAll(grpcServer);
    statsMethodHandler.bindAll(grpcServer);
    statsStatusHandler.bindAll(grpcServer);

    Handler<HttpServerRequest> chaosHandler =
        new ChaosGrpcServerInterceptor(grpcServer, chaosManager, vertx);

    grpcHealthService = new GrpcHealthService("topup").bind(grpcServer);
    return vertx.createHttpServer()
        .requestHandler(chaosHandler)
        .listen(grpcPort)
        .mapEmpty();
  }
}
