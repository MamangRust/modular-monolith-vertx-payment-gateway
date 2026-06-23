package io.example.merchant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.chaos.ChaosGrpcServerInterceptor;
import io.example.common.chaos.ChaosKafkaInterceptor;
import io.example.common.chaos.ChaosManager;
import io.example.common.chaos.ChaosSqlProxy;
import io.example.common.config.AppConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.common.config.KafkaConfig;
import io.example.merchant.repository.UserClientRepository;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.core.net.SocketAddress;
import pb.user.VertxUserQueryServiceGrpcClient;
import io.example.merchant.handler.MerchantCommandHandler;
import io.example.merchant.handler.MerchantDocumentCommandHandler;
import io.example.merchant.handler.MerchantDocumentQueryHandler;
import io.example.merchant.handler.MerchantQueryHandler;
import io.example.merchant.handler.MerchantStatsAmountHandler;
import io.example.merchant.handler.MerchantStatsMethodHandler;
import io.example.merchant.handler.MerchantStatsTotalAmountHandler;
import io.example.merchant.handler.MerchantStatsTransactionHandler;
import io.example.merchant.repository.impl.MerchantCommandRepositoryImpl;
import io.example.merchant.repository.impl.MerchantDocumentCommandRepositoryImpl;
import io.example.merchant.repository.impl.MerchantDocumentQueryRepositoryImpl;
import io.example.merchant.repository.impl.MerchantQueryRepositoryImpl;
import io.example.merchant.repository.impl.MerchantStatsAmountByApiKeyRepositoryImpl;
import io.example.merchant.repository.impl.MerchantStatsAmountByMerchantRepositoryImpl;
import io.example.merchant.repository.impl.MerchantStatsAmountRepositoryImpl;
import io.example.merchant.repository.impl.MerchantStatsMethodByApiKeyRepositoryImpl;
import io.example.merchant.repository.impl.MerchantStatsMethodByMerchantRepositoryImpl;
import io.example.merchant.repository.impl.MerchantStatsMethodRepositoryImpl;
import io.example.merchant.repository.impl.MerchantStatsTotalAmountByApiKeyRepositoryImpl;
import io.example.merchant.repository.impl.MerchantStatsTotalAmountByMerchantRepositoryImpl;
import io.example.merchant.repository.impl.MerchantStatsTotalAmountRepositoryImpl;
import io.example.merchant.repository.impl.MerchantTransactionRepositoryImpl;
import io.example.merchant.service.MerchantStatsAmountByApiKeyService;
import io.example.merchant.service.MerchantStatsAmountByMerchantService;
import io.example.merchant.service.MerchantStatsAmountService;
import io.example.merchant.service.MerchantStatsMethodByApiKeyService;
import io.example.merchant.service.MerchantStatsMethodByMerchantService;
import io.example.merchant.service.MerchantStatsMethodService;
import io.example.merchant.service.MerchantStatsTotalAmountByApiKeyService;
import io.example.merchant.service.MerchantStatsTotalAmountByMerchantService;
import io.example.merchant.service.MerchantStatsTotalAmountService;
import io.example.merchant.service.MerchantTransactionService;
import io.example.merchant.service.impl.MerchantCommandServiceImpl;
import io.example.merchant.service.impl.MerchantDocumentCommandServiceImpl;
import io.example.merchant.service.impl.MerchantDocumentQueryServiceImpl;
import io.example.merchant.service.impl.MerchantQueryServiceImpl;
import io.example.merchant.service.impl.MerchantStatsAmountByApiKeyServiceImpl;
import io.example.merchant.service.impl.MerchantStatsAmountByMerchantServiceImpl;
import io.example.merchant.service.impl.MerchantStatsAmountServiceImpl;
import io.example.merchant.service.impl.MerchantStatsMethodByApiKeyServiceImpl;
import io.example.merchant.service.impl.MerchantStatsMethodByMerchantServiceImpl;
import io.example.merchant.service.impl.MerchantStatsMethodServiceImpl;
import io.example.merchant.service.impl.MerchantStatsTotalAmountByApiKeyServiceImpl;
import io.example.merchant.service.impl.MerchantStatsTotalAmountByMerchantServiceImpl;
import io.example.merchant.service.impl.MerchantStatsTotalAmountServiceImpl;
import io.example.merchant.service.impl.MerchantTransactionServiceImpl;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class MerchantVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(MerchantVerticle.class);

  private TelemetryConfig telemetryConfig;
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
        .put("grpc_port", 8086)
        .put("user_grpc_port", 8082)
        .put("service.name", "merchant-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new MerchantVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Merchant Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 8086");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy MerchantVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Initialize Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "merchant-service");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "merchant-service");

    // 2. Initialize Repositories
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

    this.chaosManager = new ChaosManager();
    this.chaosManager.startWatcher(vertx);
    Pool chaosPool = ChaosSqlProxy.wrap(pool, chaosManager, vertx);

    var queryRepo = new MerchantQueryRepositoryImpl(chaosPool);
    var cmdRepo = new MerchantCommandRepositoryImpl(chaosPool);
    var docQueryRepo = new MerchantDocumentQueryRepositoryImpl(chaosPool);
    var docCmdRepo = new MerchantDocumentCommandRepositoryImpl(chaosPool);
    var txnRepo = new MerchantTransactionRepositoryImpl(chaosPool);

    // Method Repos
    var statsMethRepo = new MerchantStatsMethodRepositoryImpl(chaosPool);
    var statsMethApiKeyRepo = new MerchantStatsMethodByApiKeyRepositoryImpl(chaosPool);
    var statsMethMerchRepo = new MerchantStatsMethodByMerchantRepositoryImpl(chaosPool);

    // Amount Repos
    var statsAmtRepo = new MerchantStatsAmountRepositoryImpl(chaosPool);
    var statsAmtApiKeyRepo = new MerchantStatsAmountByApiKeyRepositoryImpl(chaosPool);
    var statsAmtMerchRepo = new MerchantStatsAmountByMerchantRepositoryImpl(chaosPool);

    // Total Amount Repos
    var statsTotRepo = new MerchantStatsTotalAmountRepositoryImpl(chaosPool);
    var statsTotApiKeyRepo = new MerchantStatsTotalAmountByApiKeyRepositoryImpl(chaosPool);
    var statsTotMerchRepo = new MerchantStatsTotalAmountByMerchantRepositoryImpl(chaosPool);

    // 3. Initialize Caching
    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    RedisService redisService = new RedisService(redisAPI, openTelemetry);

    // 4. Initialize Kafka (with chaos interceptor)
    KafkaProducer<String, String> producer = KafkaConfig.createProducer(vertx);
    KafkaProducer<String, String> chaosProducer = ChaosKafkaInterceptor.wrap(producer, chaosManager, vertx);
    this.kafkaService = new KafkaService(chaosProducer);

    // 5. Initialize gRPC Clients
    this.grpcClient = GrpcClient.client(vertx);
    String userHost = System.getenv().getOrDefault("USER_SERVICE_HOST", "user");
    int userPort = Integer.parseInt(System.getenv().getOrDefault("USER_SERVICE_PORT", "8083"));
    var userClient = new VertxUserQueryServiceGrpcClient(grpcClient,
        SocketAddress.inetSocketAddress(userPort, userHost));
    var userClientRepo = new UserClientRepository(userClient);

    // 6. Initialize Services
    var queryService = new MerchantQueryServiceImpl(queryRepo, redisService, tracingMetrics);
    var cmdService = new MerchantCommandServiceImpl(cmdRepo, queryRepo, userClientRepo, redisService, kafkaService,
        tracingMetrics);
    var docQueryService = new MerchantDocumentQueryServiceImpl(docQueryRepo, redisService, tracingMetrics);
    var docCmdService = new MerchantDocumentCommandServiceImpl(docCmdRepo, docQueryRepo, queryRepo, userClientRepo,
        redisService, kafkaService, tracingMetrics);

    MerchantTransactionService txnService = new MerchantTransactionServiceImpl(txnRepo, redisService, tracingMetrics);

    // Method Services
    MerchantStatsMethodService statsMethService = new MerchantStatsMethodServiceImpl(statsMethRepo, redisService,
        tracingMetrics);
    MerchantStatsMethodByApiKeyService statsMethApiKeyService = new MerchantStatsMethodByApiKeyServiceImpl(
        statsMethApiKeyRepo, redisService, tracingMetrics);
    MerchantStatsMethodByMerchantService statsMethMerchService = new MerchantStatsMethodByMerchantServiceImpl(
        statsMethMerchRepo, redisService, tracingMetrics);

    // Amount Services
    MerchantStatsAmountService statsAmtService = new MerchantStatsAmountServiceImpl(statsAmtRepo, redisService,
        tracingMetrics);
    MerchantStatsAmountByApiKeyService statsAmtApiKeyService = new MerchantStatsAmountByApiKeyServiceImpl(
        statsAmtApiKeyRepo, redisService, tracingMetrics);
    MerchantStatsAmountByMerchantService statsAmtMerchService = new MerchantStatsAmountByMerchantServiceImpl(
        statsAmtMerchRepo, redisService, tracingMetrics);

    // Total Amount Services
    MerchantStatsTotalAmountService statsTotService = new MerchantStatsTotalAmountServiceImpl(statsTotRepo,
        redisService, tracingMetrics);
    MerchantStatsTotalAmountByApiKeyService statsTotApiKeyService = new MerchantStatsTotalAmountByApiKeyServiceImpl(
        statsTotApiKeyRepo, redisService, tracingMetrics);
    MerchantStatsTotalAmountByMerchantService statsTotMerchService = new MerchantStatsTotalAmountByMerchantServiceImpl(
        statsTotMerchRepo, redisService, tracingMetrics);

    // 6. Initialize Handlers
    var queryHandler = new MerchantQueryHandler(queryService);
    var cmdHandler = new MerchantCommandHandler(cmdService);
    var docQueryHandler = new MerchantDocumentQueryHandler(docQueryService);
    var docCmdHandler = new MerchantDocumentCommandHandler(docCmdService);

    var statsTxnHandler = new MerchantStatsTransactionHandler(txnService);
    var statsAmtHandler = new MerchantStatsAmountHandler(statsAmtService, statsAmtApiKeyService, statsAmtMerchService);
    var statsMethHandler = new MerchantStatsMethodHandler(statsMethService, statsMethApiKeyService,
        statsMethMerchService);
    var statsTotHandler = new MerchantStatsTotalAmountHandler(statsTotService, statsTotApiKeyService,
        statsTotMerchService);

    int port = cfg.getGrpcPort();

    startGrpcServer(queryHandler, cmdHandler, docQueryHandler, docCmdHandler, statsTxnHandler, statsAmtHandler,
        statsMethHandler, statsTotHandler, port)
        .onSuccess(v -> {
          log.info("MerchantVerticle fully initialized. Listening for gRPC on port {}", port);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind Merchant gRPC server", err);
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
      MerchantQueryHandler queryHandler,
      MerchantCommandHandler cmdHandler,
      MerchantDocumentQueryHandler docQueryHandler,
      MerchantDocumentCommandHandler docCmdHandler,
      MerchantStatsTransactionHandler statsTxnHandler,
      MerchantStatsAmountHandler statsAmtHandler,
      MerchantStatsMethodHandler statsMethHandler,
      MerchantStatsTotalAmountHandler statsTotHandler,
      int grpcPort) {
    GrpcServer grpcServer = GrpcServer.server(vertx);

    queryHandler.bindAll(grpcServer);
    cmdHandler.bindAll(grpcServer);
    docQueryHandler.bindAll(grpcServer);
    docCmdHandler.bindAll(grpcServer);
    statsTxnHandler.bindAll(grpcServer);
    statsAmtHandler.bindAll(grpcServer);
    statsMethHandler.bindAll(grpcServer);
    statsTotHandler.bindAll(grpcServer);

    Handler<HttpServerRequest> chaosHandler =
        new ChaosGrpcServerInterceptor(grpcServer, chaosManager, vertx);

    return vertx.createHttpServer()
        .requestHandler(chaosHandler)
        .listen(grpcPort)
        .mapEmpty();
  }
}
