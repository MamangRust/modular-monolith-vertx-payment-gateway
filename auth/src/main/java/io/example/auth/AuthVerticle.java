package io.example.auth;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.auth.handler.AuthHandler;
import io.example.auth.repository.Repositories;
import io.example.auth.service.IdentityService;
import io.example.auth.service.LoginService;
import io.example.auth.service.PasswordResetService;
import io.example.auth.service.RegisterService;
import io.example.auth.service.TokenService;
import io.example.common.chaos.ChaosKafkaInterceptor;
import io.example.common.chaos.ChaosManager;
import io.example.common.chaos.ChaosSqlProxy;
import io.example.common.config.AppConfig;
import io.example.common.grpc.GrpcHealthService;
import io.example.common.config.JwtConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class AuthVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(AuthVerticle.class);

  private TelemetryConfig telemetryConfig;
  private GrpcHealthService grpcHealthService;
  private io.example.common.service.KafkaService kafkaService;

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
        .put("grpc_port", 8083)
        .put("service.name", "auth-service");
    // jwt_secret intentionally absent: resolved from JWT_SECRET/SECRET_KEY env at startup.

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new AuthVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Auth Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 8083");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy AuthVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Initialize Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "auth-service");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "auth-service");

    // 2. Initialize Repositories & Database
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

    // Initialize New Repositories
    Repositories repositories = new Repositories(chaosPool);

    // Initialize Kafka Service
    Map<String, String> kafkaConfig = new HashMap<>();
    kafkaConfig.put("bootstrap.servers", cfg.getKafkaBrokers());
    kafkaConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    kafkaConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    kafkaConfig.put("acks", "1");
    KafkaProducer<String, String> producer = KafkaProducer.create(vertx, kafkaConfig);
    KafkaProducer<String, String> chaosProducer = ChaosKafkaInterceptor.wrap(producer, chaosManager, vertx);
    this.kafkaService = new io.example.common.service.KafkaService(chaosProducer);

    // Signing key comes from JWT_SECRET (fallback SECRET_KEY) via JwtConfig, and must
    // match what the apigateway uses to verify. No hardcoded fallback: a shared constant
    // in source lets anyone forge tokens. Config key "jwt_secret" still wins for tests.
    String jwtSecret = rawConfig.containsKey("jwt_secret")
        ? rawConfig.getString("jwt_secret")
        : JwtConfig.resolveSecret();
    JWTAuth jwtProvider = JWTAuth.create(vertx, new JWTAuthOptions()
        .addPubSecKey(new PubSecKeyOptions()
            .setAlgorithm("HS256")
            .setBuffer(jwtSecret)));

    // 3. Initialize Caching
    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    RedisService redisService = new RedisService(redisAPI, openTelemetry);

    // 4. Initialize Services
    TokenService tokenService = new TokenService(jwtProvider);
    var registerService = new RegisterService(
        repositories.getUser(),
        repositories.getRole(),
        repositories.getUserRole(),
        redisService,
        tracingMetrics,
        kafkaService);
    var identityService = new IdentityService(
        repositories.getUser(),
        repositories.getRefreshToken(),
        redisService,
        tokenService,
        jwtProvider,
        tracingMetrics);
    var passwordResetService = new PasswordResetService(
        repositories.getUser(),
        repositories.getResetToken(),
        redisService,
        tracingMetrics,
        kafkaService);
    var loginService = new LoginService(
        repositories.getUser(),
        redisService,
        tokenService,
        tracingMetrics);

    // 5. Initialize Unified Handler
    AuthHandler handler = new AuthHandler(registerService, identityService, passwordResetService,
        loginService);

    int port = cfg.getGrpcPort();

    startGrpcServer(handler, port)
        .onSuccess(v -> {
          log.info("AuthVerticle fully initialized with CQRS. Listening for gRPC on port {}", port);
          grpcHealthService.setServing(true);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind Auth gRPC server", err);
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
    stopPromise.complete();
  }

  private Future<Void> startGrpcServer(AuthHandler handler, int grpcPort) {
    GrpcServer grpcServer = GrpcServer.server(vertx);

    // Bind the unified API handler onto server
    handler.bindAll(grpcServer);

    grpcHealthService = new GrpcHealthService("auth").bind(grpcServer);
    return vertx.createHttpServer()
        .requestHandler(grpcServer)
        .listen(grpcPort)
        .mapEmpty();
  }
}
