package io.example.card;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.card.handler.CardCommandHandler;
import io.example.card.handler.CardDashboardHandler;
import io.example.card.handler.CardQueryHandler;
import io.example.card.handler.CardStatsBalanceHandler;
import io.example.card.handler.CardStatsTopupHandler;
import io.example.card.handler.CardStatsTransactionHandler;
import io.example.card.handler.CardStatsTransferHandler;
import io.example.card.handler.CardStatsWithdrawHandler;
import io.example.card.repository.CardCommandRepository;
import io.example.card.repository.CardDashboardBalanceRepository;
import io.example.card.repository.CardDashboardTopupRepository;
import io.example.card.repository.CardDashboardTransactionRepository;
import io.example.card.repository.CardDashboardTransferRepository;
import io.example.card.repository.CardDashboardWithdrawRepository;
import io.example.card.repository.CardQueryRepository;
import io.example.card.repository.CardStatsBalanceByCardRepository;
import io.example.card.repository.CardStatsBalanceRepository;
import io.example.card.repository.CardStatsTopupByCardRepository;
import io.example.card.repository.CardStatsTopupRepository;
import io.example.card.repository.CardStatsTransactionByCardRepository;
import io.example.card.repository.CardStatsTransactionRepository;
import io.example.card.repository.CardStatsTransferByCardRepository;
import io.example.card.repository.CardStatsTransferRepository;
import io.example.card.repository.CardStatsWithdrawByCardRepository;
import io.example.card.repository.CardStatsWithdrawRepository;
import io.example.card.repository.UserClientRepository;
import io.example.card.repository.impl.CardCommandRepositoryImpl;
import io.example.card.repository.impl.CardDashboardBalanceRepositoryImpl;
import io.example.card.repository.impl.CardDashboardTopupRepositoryImpl;
import io.example.card.repository.impl.CardDashboardTransactionRepositoryImpl;
import io.example.card.repository.impl.CardDashboardTransferRepositoryImpl;
import io.example.card.repository.impl.CardDashboardWithdrawRepositoryImpl;
import io.example.card.repository.impl.CardQueryRepositoryImpl;
import io.example.card.repository.impl.CardStatsBalanceByCardRepositoryImpl;
import io.example.card.repository.impl.CardStatsBalanceRepositoryImpl;
import io.example.card.repository.impl.CardStatsTopupByCardRepositoryImpl;
import io.example.card.repository.impl.CardStatsTopupRepositoryImpl;
import io.example.card.repository.impl.CardStatsTransactionByCardRepositoryImpl;
import io.example.card.repository.impl.CardStatsTransactionRepositoryImpl;
import io.example.card.repository.impl.CardStatsTransferByCardRepositoryImpl;
import io.example.card.repository.impl.CardStatsTransferRepositoryImpl;
import io.example.card.repository.impl.CardStatsWithdrawByCardRepositoryImpl;
import io.example.card.repository.impl.CardStatsWithdrawRepositoryImpl;
import io.example.card.repository.impl.UserClientRepositoryImpl;
import io.example.card.service.CardCommandService;
import io.example.card.service.CardQueryService;
import io.example.card.service.CardStatsBalanceService;
import io.example.card.service.CardStatsDashboardService;
import io.example.card.service.CardStatsTopupService;
import io.example.card.service.CardStatsTransactionService;
import io.example.card.service.CardStatsTransferService;
import io.example.card.service.CardStatsWithdrawService;
import io.example.card.service.impl.CardCommandServiceImpl;
import io.example.card.service.impl.CardQueryServiceImpl;
import io.example.card.service.impl.CardStatsBalanceServiceImpl;
import io.example.card.service.impl.CardStatsDashboardServiceImpl;
import io.example.card.service.impl.CardStatsTopupServiceImpl;
import io.example.card.service.impl.CardStatsTransactionServiceImpl;
import io.example.card.service.impl.CardStatsTransferServiceImpl;
import io.example.card.service.impl.CardStatsWithdrawServiceImpl;
import io.example.common.config.AppConfig;
import io.example.common.config.KafkaConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class CardVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(CardVerticle.class);

  private TelemetryConfig telemetryConfig;
  private KafkaService kafkaService;
  private io.vertx.grpc.client.GrpcClient userGrpcClient;

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
        .put("grpc_port", 8085)
        .put("service.name", "card-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new CardVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Card Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 8085");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy CardVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Initialize Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "card-service");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "card-service");

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

    // Command & Query
    CardQueryRepository queryRepo = new CardQueryRepositoryImpl(pool);
    CardCommandRepository cmdRepo = new CardCommandRepositoryImpl(pool);

    // Dashboard Repos
    CardDashboardBalanceRepository dashBalRepo = new CardDashboardBalanceRepositoryImpl(pool);
    CardDashboardTopupRepository dashTopRepo = new CardDashboardTopupRepositoryImpl(pool);
    CardDashboardWithdrawRepository dashWitRepo = new CardDashboardWithdrawRepositoryImpl(pool);
    CardDashboardTransactionRepository dashTxnRepo = new CardDashboardTransactionRepositoryImpl(pool);
    CardDashboardTransferRepository dashTrfRepo = new CardDashboardTransferRepositoryImpl(pool);

    // Stats Repos
    CardStatsBalanceRepository balRepo = new CardStatsBalanceRepositoryImpl(pool);
    CardStatsBalanceByCardRepository balByCardRepo = new CardStatsBalanceByCardRepositoryImpl(pool);
    CardStatsTopupRepository topRepo = new CardStatsTopupRepositoryImpl(pool);
    CardStatsTopupByCardRepository topByCardRepo = new CardStatsTopupByCardRepositoryImpl(pool);
    CardStatsWithdrawRepository witRepo = new CardStatsWithdrawRepositoryImpl(pool);
    CardStatsWithdrawByCardRepository witByCardRepo = new CardStatsWithdrawByCardRepositoryImpl(pool);
    CardStatsTransactionRepository txnRepo = new CardStatsTransactionRepositoryImpl(pool);
    CardStatsTransactionByCardRepository txnByCardRepo = new CardStatsTransactionByCardRepositoryImpl(pool);
    CardStatsTransferRepository trfRepo = new CardStatsTransferRepositoryImpl(pool);
    CardStatsTransferByCardRepository trfByCardRepo = new CardStatsTransferByCardRepositoryImpl(pool);

    // 3. Initialize Caching
    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    RedisService redisService = new RedisService(redisAPI, openTelemetry);

    // 4. Initialize gRPC Clients
    String userHost = rawConfig.getString("user_host", "localhost");
    int userPort = rawConfig.getInteger("user_grpc_port", 8082);
    this.userGrpcClient = io.vertx.grpc.client.GrpcClient.client(vertx);
    var userStub = new pb.user.VertxUserQueryServiceGrpcClient(userGrpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(userPort, userHost));
    UserClientRepository userClientRepo = new UserClientRepositoryImpl(userStub);

    // 5. Initialize Kafka
    KafkaProducer<String, String> kafkaProducer = KafkaConfig.createProducer(vertx);
    this.kafkaService = new KafkaService(kafkaProducer);

    // 6. Initialize Services

    CardQueryService queryService = new CardQueryServiceImpl(queryRepo, redisService, tracingMetrics);
    CardCommandService cmdService = new CardCommandServiceImpl(cmdRepo, userClientRepo, redisService, tracingMetrics,
        kafkaService);
    CardStatsDashboardService dashService = new CardStatsDashboardServiceImpl(dashBalRepo, dashTopRepo, dashWitRepo,
        dashTxnRepo, dashTrfRepo, redisService, tracingMetrics);
    CardStatsBalanceService balService = new CardStatsBalanceServiceImpl(balRepo, balByCardRepo, redisService,
        tracingMetrics);
    CardStatsTopupService topService = new CardStatsTopupServiceImpl(topRepo, topByCardRepo, redisService,
        tracingMetrics);
    CardStatsWithdrawService witService = new CardStatsWithdrawServiceImpl(witRepo, witByCardRepo, redisService,
        tracingMetrics);
    CardStatsTransactionService txnService = new CardStatsTransactionServiceImpl(txnRepo, txnByCardRepo,
        redisService, tracingMetrics);
    CardStatsTransferService trfService = new CardStatsTransferServiceImpl(trfRepo, trfByCardRepo, redisService,
        tracingMetrics);

    // 6. Initialize Handlers
    var queryHandler = new CardQueryHandler(queryService);
    var cmdHandler = new CardCommandHandler(cmdService);
    var dashHandler = new CardDashboardHandler(dashService);
    var balHandler = new CardStatsBalanceHandler(balService);
    var topHandler = new CardStatsTopupHandler(topService);
    var witHandler = new CardStatsWithdrawHandler(witService);
    var txnHandler = new CardStatsTransactionHandler(txnService);
    var trfHandler = new CardStatsTransferHandler(trfService);

    int port = cfg.getGrpcPort();

    startGrpcServer(queryHandler, cmdHandler, dashHandler, balHandler, topHandler, witHandler, txnHandler, trfHandler,
        port)
        .onSuccess(v -> {
          log.info("CardVerticle fully initialized with Granular Repositories. Listening for gRPC on port {}", port);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind Card gRPC server", err);
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
    if (userGrpcClient != null) {
      userGrpcClient.close();
    }
    stopPromise.complete();
  }

  private Future<Void> startGrpcServer(
      CardQueryHandler queryHandler,
      CardCommandHandler cmdHandler,
      CardDashboardHandler dashHandler,
      CardStatsBalanceHandler balHandler,
      CardStatsTopupHandler topHandler,
      CardStatsWithdrawHandler witHandler,
      CardStatsTransactionHandler txnHandler,
      CardStatsTransferHandler trfHandler,
      int grpcPort) {
    GrpcServer grpcServer = GrpcServer.server(vertx);

    queryHandler.bindAll(grpcServer);
    cmdHandler.bindAll(grpcServer);

    dashHandler.bindAll(grpcServer);
    balHandler.bindAll(grpcServer);
    topHandler.bindAll(grpcServer);
    witHandler.bindAll(grpcServer);
    txnHandler.bindAll(grpcServer);
    trfHandler.bindAll(grpcServer);

    return vertx.createHttpServer()
        .requestHandler(grpcServer)
        .listen(grpcPort)
        .mapEmpty();
  }
}
