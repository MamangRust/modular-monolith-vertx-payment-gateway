package io.example.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.chaos.ChaosGrpcServerInterceptor;
import io.example.common.chaos.ChaosKafkaInterceptor;
import io.example.common.chaos.ChaosManager;
import io.example.common.chaos.ChaosSqlProxy;
import io.example.common.config.AppConfig;
import io.example.common.config.KafkaConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.transaction.handler.TransactionCommandHandler;
import io.example.transaction.handler.TransactionQueryHandler;
import io.example.transaction.handler.TransactionStatsAmountHandler;
import io.example.transaction.handler.TransactionStatsMethodHandler;
import io.example.transaction.handler.TransactionStatsStatusHandler;
import io.example.transaction.repository.CardClientRepository;
import io.example.transaction.repository.MerchantClientRepository;
import io.example.transaction.repository.SaldoClientRepository;
import io.example.transaction.repository.TransactionCommandRepository;
import io.example.transaction.repository.TransactionQueryRepository;
import io.example.transaction.repository.TransactionStatsAmountRepository;
import io.example.transaction.repository.TransactionStatsMethodRepository;
import io.example.transaction.repository.TransactionStatsStatusRepository;
import io.example.transaction.repository.impl.TransactionCommandRepositoryImpl;
import io.example.transaction.repository.impl.TransactionQueryRepositoryImpl;
import io.example.transaction.repository.impl.TransactionStatsAmountRepositoryImpl;
import io.example.transaction.repository.impl.TransactionStatsMethodRepositoryImpl;
import io.example.transaction.repository.impl.TransactionStatsStatusRepositoryImpl;
import io.example.transaction.service.TransactionCommandService;
import io.example.transaction.service.TransactionQueryService;
import io.example.transaction.service.TransactionStatsAmountService;
import io.example.transaction.service.TransactionStatsMethodService;
import io.example.transaction.service.TransactionStatsStatusService;
import io.example.transaction.service.impl.TransactionCommandServiceImpl;
import io.example.transaction.service.impl.TransactionQueryServiceImpl;
import io.example.transaction.service.impl.TransactionStatsAmountServiceImpl;
import io.example.transaction.service.impl.TransactionStatsMethodServiceImpl;
import io.example.transaction.service.impl.TransactionStatsStatusServiceImpl;
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
import pb.card.VertxCardQueryServiceGrpcClient;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;
import pb.saldo.VertxSaldoCommandServiceGrpcClient;
import pb.saldo.VertxSaldoQueryServiceGrpcClient;

public class TransactionVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(TransactionVerticle.class);

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
        .put("grpc_port", 8090)
        .put("card_service_host", "localhost")
        .put("card_service_port", 8082)
        .put("merchant_service_host", "localhost")
        .put("merchant_service_port", 8083)
        .put("saldo_service_host", "localhost")
        .put("saldo_service_port", 8084)
        .put("service.name", "transaction-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new TransactionVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Transaction Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 8090");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy TransactionVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Telemetry Initialize
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "transaction-service");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "transaction-service");

    // 2. DB Repository Setup
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

    // CQRS Repositories
    TransactionQueryRepository queryRepo = new TransactionQueryRepositoryImpl(chaosPool);
    TransactionCommandRepository cmdRepo = new TransactionCommandRepositoryImpl(chaosPool);
    TransactionStatsAmountRepository statsAmtRepo = new TransactionStatsAmountRepositoryImpl(chaosPool);
    TransactionStatsMethodRepository statsMethRepo = new TransactionStatsMethodRepositoryImpl(chaosPool);
    TransactionStatsStatusRepository statsStatusRepo = new TransactionStatsStatusRepositoryImpl(chaosPool);

    // 3. gRPC Clients Initialize
    this.grpcClient = GrpcClient.client(vertx);

    String cardHost = rawConfig.getString("card_service_host", "localhost");
    int cardPort = rawConfig.getInteger("card_service_port", 8082);
    var cardStub = new VertxCardQueryServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(cardPort, cardHost));
    var cardClientRepo = new CardClientRepository(cardStub);

    String merchantHost = rawConfig.getString("merchant_service_host", "localhost");
    int merchantPort = rawConfig.getInteger("merchant_service_port", 8083);
    var merchantStub = new VertxMerchantQueryServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(merchantPort, merchantHost));
    var merchantClientRepo = new MerchantClientRepository(merchantStub);

    String saldoHost = rawConfig.getString("saldo_service_host", "localhost");
    int saldoPort = rawConfig.getInteger("saldo_service_port", 8084);
    var saldoQueryStub = new VertxSaldoQueryServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(saldoPort, saldoHost));
    var saldoCmdStub = new VertxSaldoCommandServiceGrpcClient(grpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(saldoPort, saldoHost));
    var saldoClientRepo = new SaldoClientRepository(saldoQueryStub, saldoCmdStub);

    // 4. Redis Setup
    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    RedisService redisService = new RedisService(redisAPI, openTelemetry);

    // 5. Kafka Setup (with chaos interceptor)
    io.vertx.kafka.client.producer.KafkaProducer<String, String> kafkaProducer = KafkaConfig.createProducer(vertx);
    io.vertx.kafka.client.producer.KafkaProducer<String, String> chaosKafkaProducer =
        ChaosKafkaInterceptor.wrap(kafkaProducer, chaosManager, vertx);
    this.kafkaService = new KafkaService(chaosKafkaProducer);

    // 6. CQRS Services Setup
    TransactionQueryService queryService = new TransactionQueryServiceImpl(queryRepo, redisService, tracingMetrics);
    TransactionCommandService cmdService = new TransactionCommandServiceImpl(cmdRepo, queryRepo, merchantClientRepo,
        cardClientRepo, saldoClientRepo, redisService, kafkaService, tracingMetrics);
    TransactionStatsAmountService statsAmtService = new TransactionStatsAmountServiceImpl(statsAmtRepo, redisService,
        tracingMetrics);
    TransactionStatsMethodService statsMethService = new TransactionStatsMethodServiceImpl(statsMethRepo, redisService,
        tracingMetrics);
    TransactionStatsStatusService statsStatusService = new TransactionStatsStatusServiceImpl(statsStatusRepo,
        redisService, tracingMetrics);

    // 6. CQRS Handlers Setup
    var queryHandler = new TransactionQueryHandler(queryService);
    var cmdHandler = new TransactionCommandHandler(cmdService);
    var statsAmtHandler = new TransactionStatsAmountHandler(statsAmtService);
    var statsMethHandler = new TransactionStatsMethodHandler(statsMethService);
    var statsStatusHandler = new TransactionStatsStatusHandler(statsStatusService);

    int port = cfg.getGrpcPort();

    startGrpcServer(queryHandler, cmdHandler, statsAmtHandler, statsMethHandler, statsStatusHandler, port)
        .onSuccess(v -> {
          log.info(
              "TransactionVerticle fully initialized with Decoupled CQRS and gRPC Clients. Listening for gRPC on port {}",
              port);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind Transaction gRPC server", err);
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
      TransactionQueryHandler queryHandler,
      TransactionCommandHandler cmdHandler,
      TransactionStatsAmountHandler statsAmtHandler,
      TransactionStatsMethodHandler statsMethHandler,
      TransactionStatsStatusHandler statsStatusHandler,
      int grpcPort) {
    GrpcServer grpcServer = GrpcServer.server(vertx);

    queryHandler.bindAll(grpcServer);
    cmdHandler.bindAll(grpcServer);
    statsAmtHandler.bindAll(grpcServer);
    statsMethHandler.bindAll(grpcServer);
    statsStatusHandler.bindAll(grpcServer);

    // Wrap with gRPC chaos interceptor
    Handler<HttpServerRequest> chaosHandler =
        new ChaosGrpcServerInterceptor(grpcServer, chaosManager, vertx);

    return vertx.createHttpServer()
        .requestHandler(chaosHandler)
        .listen(grpcPort)
        .mapEmpty();
  }
}
