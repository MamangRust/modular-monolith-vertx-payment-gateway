package io.example.saldo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.config.AppConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
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

public class SaldoVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(SaldoVerticle.class);

  private TelemetryConfig telemetryConfig;
  private io.vertx.grpc.client.GrpcClient cardGrpcClient;

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
        .put("grpc_port", 8084)
        .put("card_service_host", "localhost")
        .put("card_service_port", 8082)
        .put("service.name", "saldo-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new SaldoVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Saldo Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 8084");
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
        .setPassword(dbCfg.getString("password", "vertx"));

    PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(dbCfg.getInteger("pool_size", 5));

    Pool pool = Pool.pool(vertx, connectOptions, poolOptions);

    SaldoQueryRepository queryRepo = new SaldoQueryRepositoryImpl(pool);
    SaldoCommandRepository cmdRepo = new SaldoCommandRepositoryImpl(pool);
    SaldoStatsTotalRepository statsTotalRepo = new SaldoStatsTotalRepositoryImpl(pool);
    SaldoStatsBalanceRepository statsBalRepo = new SaldoStatsBalanceRepositoryImpl(pool);

    // 3. Initialize gRPC Client for Card Service
    String cardHost = rawConfig.getString("card_service_host", "localhost");
    int cardPort = rawConfig.getInteger("card_service_port", 8082);
    this.cardGrpcClient = GrpcClient.client(vertx);
    var cardStub = new VertxCardQueryServiceGrpcClient(cardGrpcClient,
        io.vertx.core.net.SocketAddress.inetSocketAddress(cardPort, cardHost));
    var cardClientRepo = new CardClientRepository(cardStub);

    // 4. Initialize Caching
    RedisAPI redisAPI = RedisConfig.createClient(vertx);

    RedisService redisService = new RedisService(redisAPI, openTelemetry);

    // 5. Initialize Services
    SaldoQueryService queryService = new SaldoQueryServiceImpl(queryRepo, statsTotalRepo, statsBalRepo, redisService,
        tracingMetrics);
    SaldoCommandService cmdService = new SaldoCommandServiceImpl(cmdRepo, cardClientRepo, redisService, tracingMetrics);
    SaldoStatsBalanceService statsBalService = new SaldoStatsBalanceServiceImpl(statsBalRepo, redisService, tracingMetrics);
    SaldoStatsTotalService statsTotalService = new SaldoStatsTotalServiceImpl(statsTotalRepo, redisService, tracingMetrics);

    // 6. Initialize Handlers
    var queryHandler = new SaldoQueryHandler(queryService);
    var cmdHandler = new SaldoCommandHandler(cmdService);
    var statsBalHandler = new SaldoStatsBalanceHandler(statsBalService);
    var statsTotalHandler = new SaldoStatsTotalHandler(statsTotalService);

    int port = cfg.getGrpcPort();

    startGrpcServer(queryHandler, cmdHandler, statsBalHandler, statsTotalHandler, port)
        .onSuccess(v -> {
          log.info("SaldoVerticle fully initialized with Decoupled CQRS and gRPC Client. Listening for gRPC on port {}",
              port);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind Saldo gRPC server", err);

          startPromise.fail(err);
        });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (telemetryConfig != null) {
      telemetryConfig.shutdown();
    }
    if (cardGrpcClient != null) {
      cardGrpcClient.close();
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

    return vertx.createHttpServer()
        .requestHandler(grpcServer)
        .listen(grpcPort)
        .mapEmpty();
  }
}
