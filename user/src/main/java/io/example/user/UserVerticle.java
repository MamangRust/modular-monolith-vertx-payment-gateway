package io.example.user;

import io.example.common.config.AppConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.user.handler.UserCommandHandler;
import io.example.user.handler.UserQueryHandler;
import io.example.user.repository.UserCommandRepository;
import io.example.user.repository.UserQueryRepository;
import io.example.user.repository.impl.UserCommandRepositoryImpl;
import io.example.user.repository.impl.UserQueryRepositoryImpl;
import io.example.user.service.UserCommandService;
import io.example.user.service.UserQueryService;
import io.example.user.service.impl.UserCommandServiceImpl;
import io.example.user.service.impl.UserQueryServiceImpl;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(UserVerticle.class);

  private TelemetryConfig telemetryConfig;

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
        .put("grpc_port", 8082)
        .put("service.name", "user-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new UserVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ User Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 8082");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy UserVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Initialize Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "user-service");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "user-service");

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
    
    UserQueryRepository queryRepo = new UserQueryRepositoryImpl(pool);
    UserCommandRepository cmdRepo = new UserCommandRepositoryImpl(pool);

    // 3. Initialize Caching
    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    RedisService redisService = new RedisService(redisAPI, openTelemetry);

    // 4. Initialize Services
    UserQueryService queryService = new UserQueryServiceImpl(queryRepo, redisService, tracingMetrics);
    UserCommandService cmdService = new UserCommandServiceImpl(cmdRepo, redisService, tracingMetrics);

    // 5. Initialize Handlers
    var queryHandler = new UserQueryHandler(queryService);
    var cmdHandler = new UserCommandHandler(cmdService);

    int port = cfg.getGrpcPort();

    startGrpcServer(queryHandler, cmdHandler, port)
        .onSuccess(v -> {
          log.info("UserVerticle fully initialized with CQRS. Listening for gRPC on port {}", port);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind User gRPC server", err);
          startPromise.fail(err);
        });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (telemetryConfig != null) {
      telemetryConfig.shutdown();
    }
    stopPromise.complete();
  }

  private Future<Void> startGrpcServer(UserQueryHandler queryHandler, UserCommandHandler cmdHandler, int grpcPort) {
    GrpcServer grpcServer = GrpcServer.server(vertx);

    queryHandler.bindAll(grpcServer);
    cmdHandler.bindAll(grpcServer);

    return vertx.createHttpServer()
        .requestHandler(grpcServer)
        .listen(grpcPort)
        .mapEmpty();
  }
}
