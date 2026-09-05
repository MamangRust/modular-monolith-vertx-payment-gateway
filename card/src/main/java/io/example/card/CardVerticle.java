package io.example.card;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.card.handler.CardAuthorizationHandler;
import io.example.card.handler.CardBillingHandler;
import io.example.card.handler.CardCommandHandler;
import io.example.card.handler.CardDashboardHandler;
import io.example.card.handler.CardLimitHandler;
import io.example.card.handler.CardPaymentHandler;
import io.example.card.handler.CardQueryHandler;
import io.example.card.handler.CardStatsBalanceHandler;
import io.example.card.handler.CardStatsTopupHandler;
import io.example.card.handler.CardStatsTransactionHandler;
import io.example.card.handler.CardStatsTransferHandler;
import io.example.card.handler.CardStatsWithdrawHandler;
import io.example.card.repository.BillingStatementRepository;
import io.example.card.repository.CardAuthTransactionRepository;
import io.example.card.repository.CardCommandRepository;
import io.example.card.repository.CardCreditAccountRepository;
import io.example.card.repository.CardDashboardBalanceRepository;
import io.example.card.repository.CardDashboardTopupRepository;
import io.example.card.repository.CardDashboardTransactionRepository;
import io.example.card.repository.CardDashboardTransferRepository;
import io.example.card.repository.CardDashboardWithdrawRepository;
import io.example.card.repository.CardPaymentRepository;
import io.example.card.repository.CardQueryRepository;
import io.example.card.repository.CardRewardRepository;
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
import io.example.card.repository.impl.BillingStatementRepositoryImpl;
import io.example.card.repository.impl.CardAuthTransactionRepositoryImpl;
import io.example.card.repository.impl.CardCommandRepositoryImpl;
import io.example.card.repository.impl.CardCreditAccountRepositoryImpl;
import io.example.card.repository.impl.CardDashboardBalanceRepositoryImpl;
import io.example.card.repository.impl.CardDashboardTopupRepositoryImpl;
import io.example.card.repository.impl.CardDashboardTransactionRepositoryImpl;
import io.example.card.repository.impl.CardDashboardTransferRepositoryImpl;
import io.example.card.repository.impl.CardDashboardWithdrawRepositoryImpl;
import io.example.card.repository.impl.CardPaymentRepositoryImpl;
import io.example.card.repository.impl.CardQueryRepositoryImpl;
import io.example.card.repository.impl.CardRewardRepositoryImpl;
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
import io.example.card.service.BillingEngineService;
import io.example.card.service.CardAuthorizationService;
import io.example.card.service.CardCommandService;
import io.example.card.service.CardPaymentService;
import io.example.card.service.CardQueryService;
import io.example.card.service.CardRewardService;
import io.example.card.service.CardStatsBalanceService;
import io.example.card.service.CardStatsDashboardService;
import io.example.card.service.CardStatsTopupService;
import io.example.card.service.CardStatsTransactionService;
import io.example.card.service.CardStatsTransferService;
import io.example.card.service.CardStatsWithdrawService;
import io.example.card.service.CreditLimitService;
import io.example.card.service.impl.BillingEngineServiceImpl;
import io.example.card.service.impl.CardAuthorizationServiceImpl;
import io.example.card.service.impl.CardCommandServiceImpl;
import io.example.card.service.impl.CardPaymentServiceImpl;
import io.example.card.service.impl.CardQueryServiceImpl;
import io.example.card.service.impl.CardRewardServiceImpl;
import io.example.card.service.impl.CardStatsBalanceServiceImpl;
import io.example.card.service.impl.CardStatsDashboardServiceImpl;
import io.example.card.service.impl.CardStatsTopupServiceImpl;
import io.example.card.service.impl.CardStatsTransactionServiceImpl;
import io.example.card.service.impl.CardStatsTransferServiceImpl;
import io.example.card.service.impl.CardStatsWithdrawServiceImpl;
import io.example.card.service.impl.CreditLimitServiceImpl;
import io.example.card.verticle.BillingSchedulerVerticle;
import io.example.card.verticle.CardEventLogVerticle;
import io.example.card.verticle.FraudScoringConsumerVerticle;
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
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
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
  private GrpcHealthService grpcHealthService;
  private KafkaService kafkaService;
  private io.vertx.grpc.client.GrpcClient userGrpcClient;
  private ChaosManager chaosManager;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    JsonObject config = new JsonObject()
        .put("database", new JsonObject()
            .put("host", System.getenv().getOrDefault("DB_HOST", "postgres"))
            .put("port", Integer.parseInt(System.getenv().getOrDefault("DB_PORT", "5432")))
            .put("database", System.getenv().getOrDefault("DB_NAME", "PAYMENT_GATEWAY"))
            .put("user", System.getenv().getOrDefault("DB_USERNAME", "vertx"))
            .put("password", System.getenv().getOrDefault("DB_PASSWORD", "vertx"))
            .put("pool_size", Integer.parseInt(System.getenv().getOrDefault("DB_POOL_SIZE", "5"))))
        .put("grpc_port", Integer.parseInt(System.getenv().getOrDefault("GRPC_PORT", "8085")))
        .put("service.name", "card-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new CardVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Card Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port {}", config.getInteger("grpc_port"));
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

    // Command & Query
    CardQueryRepository queryRepo = new CardQueryRepositoryImpl(chaosPool);
    CardCommandRepository cmdRepo = new CardCommandRepositoryImpl(chaosPool);

    // Dashboard Repos
    CardDashboardBalanceRepository dashBalRepo = new CardDashboardBalanceRepositoryImpl(chaosPool);
    CardDashboardTopupRepository dashTopRepo = new CardDashboardTopupRepositoryImpl(chaosPool);
    CardDashboardWithdrawRepository dashWitRepo = new CardDashboardWithdrawRepositoryImpl(chaosPool);
    CardDashboardTransactionRepository dashTxnRepo = new CardDashboardTransactionRepositoryImpl(chaosPool);
    CardDashboardTransferRepository dashTrfRepo = new CardDashboardTransferRepositoryImpl(chaosPool);

    // Stats Repos
    CardStatsBalanceRepository balRepo = new CardStatsBalanceRepositoryImpl(chaosPool);
    CardStatsBalanceByCardRepository balByCardRepo = new CardStatsBalanceByCardRepositoryImpl(chaosPool);
    CardStatsTopupRepository topRepo = new CardStatsTopupRepositoryImpl(chaosPool);
    CardStatsTopupByCardRepository topByCardRepo = new CardStatsTopupByCardRepositoryImpl(chaosPool);
    CardStatsWithdrawRepository witRepo = new CardStatsWithdrawRepositoryImpl(chaosPool);
    CardStatsWithdrawByCardRepository witByCardRepo = new CardStatsWithdrawByCardRepositoryImpl(chaosPool);
    CardStatsTransactionRepository txnRepo = new CardStatsTransactionRepositoryImpl(chaosPool);
    CardStatsTransactionByCardRepository txnByCardRepo = new CardStatsTransactionByCardRepositoryImpl(chaosPool);
    CardStatsTransferRepository trfRepo = new CardStatsTransferRepositoryImpl(chaosPool);
    CardStatsTransferByCardRepository trfByCardRepo = new CardStatsTransferByCardRepositoryImpl(chaosPool);

    // Card Lifecycle Repos (credit card features)
    CardCreditAccountRepository creditAccountRepo = new CardCreditAccountRepositoryImpl(chaosPool);
    CardAuthTransactionRepository authTxnRepo = new CardAuthTransactionRepositoryImpl(chaosPool);
    BillingStatementRepository billingStmtRepo = new BillingStatementRepositoryImpl(chaosPool);
    CardPaymentRepository paymentRepo = new CardPaymentRepositoryImpl(chaosPool);
    CardRewardRepository rewardRepo = new CardRewardRepositoryImpl(chaosPool);

    // 3. Initialize Caching
    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    RedisService redisService = new RedisService(redisAPI, openTelemetry);

    // 4. Initialize gRPC Clients
    // Env wins over config: CardVerticle.main() never populates user_host/user_grpc_port,
    // so the old config-only defaults (localhost:8082) pointed nowhere in K8s/Docker,
    // where the user service is reachable at user:50055.
    String userHost = System.getenv().getOrDefault("USER_SERVICE_HOST",
        rawConfig.getString("user_host", "user"));
    int userPort = Integer.parseInt(
        System.getenv().getOrDefault("USER_SERVICE_PORT",
            System.getenv().getOrDefault("GRPC_USER_PORT",
                String.valueOf(rawConfig.getInteger("user_grpc_port", 50055)))));
    this.userGrpcClient = io.vertx.grpc.client.GrpcClient.client(vertx);
    var userStub = new pb.user.VertxUserQueryServiceGrpcClient(userGrpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(userPort, userHost));
    UserClientRepository userClientRepo = new UserClientRepositoryImpl(userStub);

    // 5. Initialize Kafka (with chaos interceptor)
    KafkaProducer<String, String> kafkaProducer = KafkaConfig.createProducer(vertx);
    KafkaProducer<String, String> chaosKafkaProducer = ChaosKafkaInterceptor.wrap(kafkaProducer, chaosManager, vertx);
    this.kafkaService = new KafkaService(chaosKafkaProducer);

    // 6. Initialize Services

    CardQueryService queryService = new CardQueryServiceImpl(queryRepo, redisService, tracingMetrics);
    CardCommandService cmdService = new CardCommandServiceImpl(cmdRepo, queryRepo, userClientRepo, redisService,
        tracingMetrics,
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

    // Card Lifecycle Services (credit card features)
    CardAuthorizationService authService = new CardAuthorizationServiceImpl(
        creditAccountRepo, authTxnRepo, redisService, tracingMetrics, kafkaService);
    CardPaymentService paymentService = new CardPaymentServiceImpl(
        paymentRepo, creditAccountRepo, redisService, tracingMetrics, kafkaService);
    BillingEngineService billingService = new BillingEngineServiceImpl(
        creditAccountRepo, billingStmtRepo, tracingMetrics, kafkaService);
    CreditLimitService limitService = new CreditLimitServiceImpl(
        creditAccountRepo, redisService, tracingMetrics, kafkaService);
    CardRewardService rewardService = new CardRewardServiceImpl(
        rewardRepo, redisService, tracingMetrics);

    // 6. Initialize Handlers
    var queryHandler = new CardQueryHandler(queryService);
    var cmdHandler = new CardCommandHandler(cmdService);
    var dashHandler = new CardDashboardHandler(dashService);
    var balHandler = new CardStatsBalanceHandler(balService);
    var topHandler = new CardStatsTopupHandler(topService);
    var witHandler = new CardStatsWithdrawHandler(witService);
    var txnHandler = new CardStatsTransactionHandler(txnService);
    var trfHandler = new CardStatsTransferHandler(trfService);

    // Card Lifecycle Handlers
    var authHandler = new CardAuthorizationHandler(authService);
    var paymentHandler = new CardPaymentHandler(paymentService);
    var billingHandler = new CardBillingHandler(billingService);
    var limitHandler = new CardLimitHandler(limitService);

    int port = cfg.getGrpcPort();

    startGrpcServer(queryHandler, cmdHandler, dashHandler, balHandler, topHandler, witHandler, txnHandler, trfHandler,
        authHandler, paymentHandler, billingHandler, limitHandler, port)
        .compose(v -> {
          // Deploy worker verticles for async processing
          // Share the main verticle's database config verbatim. Using a null
          // value for "database" breaks AppConfig.getDatabaseConfig() (Vert.x
          // returns null, not the default, for a present-but-null value).
          JsonObject databaseConfig = rawConfig.getJsonObject("database");
          JsonObject workerConfig = new JsonObject()
              .put("database", databaseConfig != null ? databaseConfig.copy() : new JsonObject())
              .put("service.name", "card-worker");

          DeploymentOptions workerOpts = new DeploymentOptions()
              .setConfig(workerConfig)
              .setWorker(true)
              .setInstances(1);

          return vertx.deployVerticle(new FraudScoringConsumerVerticle(), workerOpts)
              .compose(id -> {
                log.info("✅ FraudScoringConsumerVerticle deployed as worker: {}", id);
                return vertx.deployVerticle(new BillingSchedulerVerticle(), workerOpts);
              })
              .compose(id -> {
                log.info("✅ BillingSchedulerVerticle deployed as worker: {}", id);
                return vertx.deployVerticle(new CardEventLogVerticle(), workerOpts);
              })
              .map(id -> {
                log.info("✅ CardEventLogVerticle deployed as worker: {}", id);
                return (Void) null;
              })
              .otherwise(err -> {
                log.warn("⚠️ Worker verticle deployment failed (non-fatal): {}", err.getMessage());
                return null;
              });
        })
        .onSuccess(v -> {
          log.info("CardVerticle fully initialized with credit lifecycle features. Listening for gRPC on port {}", port);
          grpcHealthService.setServing(true);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind Card gRPC server", err);
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
      CardAuthorizationHandler authHandler,
      CardPaymentHandler paymentHandler,
      CardBillingHandler billingHandler,
      CardLimitHandler limitHandler,
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

    authHandler.bindAll(grpcServer);
    paymentHandler.bindAll(grpcServer);
    billingHandler.bindAll(grpcServer);
    limitHandler.bindAll(grpcServer);

    Handler<HttpServerRequest> chaosHandler =
        new ChaosGrpcServerInterceptor(grpcServer, chaosManager, vertx);

    grpcHealthService = new GrpcHealthService("card").bind(grpcServer);
    return vertx.createHttpServer()
        .requestHandler(chaosHandler)
        .listen(grpcPort)
        .mapEmpty();
  }
}
