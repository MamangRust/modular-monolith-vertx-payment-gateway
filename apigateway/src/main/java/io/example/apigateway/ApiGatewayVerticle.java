package io.example.apigateway;

import io.example.apigateway.handler.*;
import io.example.apigateway.routes.GatewayRoutes;
import io.example.common.config.JwtConfig;
import io.example.common.config.TelemetryConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.grpc.client.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiGatewayVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(ApiGatewayVerticle.class);

  private TelemetryConfig telemetryConfig;
  private GrpcClient grpcClient;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    JsonObject config = new JsonObject()
        .put("http_port", 8080)
        .put("service.name", "api-gateway");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new ApiGatewayVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ API Gateway successfully deployed! ID: {}", id);
          log.info("🚀 Gateway listening on port 8080");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy ApiGatewayVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Setup OpenTelemetry for edge monitoring
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "api-gateway");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    telemetryConfig.initialize();

    // 2. Instantiate unified gRPC Client pool
    grpcClient = GrpcClient.client(vertx);

    // 3. Define all SocketAddresses for backend microservices using environment variables
    SocketAddress addrUser = resolveGrpcAddress("USER", "user", 8083);
    SocketAddress addrAuth = SocketAddress.inetSocketAddress(8083, "auth"); // Special case for auth as it might be used differently
    SocketAddress addrRole = resolveGrpcAddress("ROLE", "role", 8083);
    SocketAddress addrSaldo = resolveGrpcAddress("SALDO", "saldo", 8083);
    SocketAddress addrCard = resolveGrpcAddress("CARD", "card", 8083);
    SocketAddress addrMerchant = resolveGrpcAddress("MERCHANT", "merchant", 8083);
    SocketAddress addrTopup = resolveGrpcAddress("TOPUP", "topup", 8083);
    SocketAddress addrTransfer = resolveGrpcAddress("TRANSFER", "transfer", 8083);
    SocketAddress addrWithdraw = resolveGrpcAddress("WITHDRAW", "withdraw", 8083);
    SocketAddress addrTransaction = resolveGrpcAddress("TRANSACTION", "transaction", 8083);

    // 4. Instantiate client stubs pointing to target address channels
    // User
    var userQuery = new pb.user.VertxUserQueryServiceGrpcClient(grpcClient, addrUser);
    var userCmd = new pb.user.VertxUserCommandServiceGrpcClient(grpcClient, addrUser);

    // Auth
    var authClient = new pb.VertxAuthServiceGrpcClient(grpcClient, addrAuth);

    // Role
    var roleQuery = new pb.role.VertxRoleServiceGrpcClient(grpcClient, addrRole);
    var roleCmd = new pb.role.VertxRoleCommandServiceGrpcClient(grpcClient, addrRole);

    // Saldo
    var saldoQuery = new pb.saldo.VertxSaldoQueryServiceGrpcClient(grpcClient, addrSaldo);
    var saldoCmd = new pb.saldo.VertxSaldoCommandServiceGrpcClient(grpcClient, addrSaldo);
    var saldoStatsBal = new pb.saldo.stats.VertxSaldoStatsBalanceServiceGrpcClient(grpcClient, addrSaldo);
    var saldoStatsTot = new pb.saldo.stats.VertxSaldoStatsTotalBalanceGrpcClient(grpcClient, addrSaldo);

    // Card
    var cardQuery = new pb.card.VertxCardQueryServiceGrpcClient(grpcClient, addrCard);
    var cardCmd = new pb.card.VertxCardCommandServiceGrpcClient(grpcClient, addrCard);
    var cardDash = new pb.card.VertxCardDashboardServiceGrpcClient(grpcClient, addrCard);
    var cardStatsBal = new pb.card.stats.VertxCardStatsBalanceServiceGrpcClient(grpcClient, addrCard);
    var cardStatsTop = new pb.card.stats.VertxCardStatsTopupServiceGrpcClient(grpcClient, addrCard);
    var cardStatsWit = new pb.card.stats.VertxCardStatsWithdrawServiceGrpcClient(grpcClient, addrCard);
    var cardStatsTxn = new pb.card.stats.VertxCardStatsTransactionServiceGrpcClient(grpcClient, addrCard);
    var cardStatsTrf = new pb.card.stats.VertxCardStatsTransferServiceGrpcClient(grpcClient, addrCard);

    // Merchant
    var merchantQuery = new pb.merchant.VertxMerchantQueryServiceGrpcClient(grpcClient, addrMerchant);
    var merchantCmd = new pb.merchant.VertxMerchantCommandServiceGrpcClient(grpcClient, addrMerchant);
    var merchantDocCmd = new pb.merchant_document.VertxMerchantDocumentCommandServiceGrpcClient(grpcClient, addrMerchant);
    var merchantDocQuery = new pb.merchant_document.VertxMerchantDocumentQueryServiceGrpcClient(grpcClient, addrMerchant);
    var merchantStatsAmt = new pb.merchant.stats.VertxMerchantStatsAmountServiceGrpcClient(grpcClient, addrMerchant);
    var merchantStatsMet = new pb.merchant.stats.VertxMerchantStatsMethodServiceGrpcClient(grpcClient, addrMerchant);
    var merchantStatsTot = new pb.merchant.stats.VertxMerchantStatsTotalAmountServiceGrpcClient(grpcClient, addrMerchant);
    var merchantTxn = new pb.merchant.VertxMerchantTransactionServiceGrpcClient(grpcClient, addrMerchant);

    // Topup
    var topupQuery = new pb.topup.VertxTopupQueryServiceGrpcClient(grpcClient, addrTopup);
    var topupCmd = new pb.topup.VertxTopupCommandServiceGrpcClient(grpcClient, addrTopup);
    var topupStatsAmt = new pb.topup.stats.VertxTopupStatsAmountServiceGrpcClient(grpcClient, addrTopup);
    var topupStatsMet = new pb.topup.stats.VertxTopupStatsMethodServiceGrpcClient(grpcClient, addrTopup);
    var topupStatsSts = new pb.topup.stats.VertxTopupStatsStatusServiceGrpcClient(grpcClient, addrTopup);

    // Transfer
    var transferQuery = new pb.transfer.VertxTransferQueryServiceGrpcClient(grpcClient, addrTransfer);
    var transferCmd = new pb.transfer.VertxTransferCommandServiceGrpcClient(grpcClient, addrTransfer);
    var transferStatsAmt = new pb.transfer.stats.VertxTransferStatsAmountServiceGrpcClient(grpcClient, addrTransfer);
    var transferStatsSts = new pb.transfer.stats.VertxTransferStatsStatusServiceGrpcClient(grpcClient, addrTransfer);

    // Withdraw
    var withdrawQuery = new pb.withdraw.VertxWithdrawQueryServiceGrpcClient(grpcClient, addrWithdraw);
    var withdrawCmd = new pb.withdraw.VertxWithdrawCommandServiceGrpcClient(grpcClient, addrWithdraw);
    var withdrawStatsAmount = new pb.withdraw.stats.VertxWithdrawStatsAmountServiceGrpcClient(grpcClient, addrWithdraw);
    var withdrawStatsStatus = new pb.withdraw.stats.VertxWithdrawStatsStatusServiceGrpcClient(grpcClient, addrWithdraw);

    // Transaction
    var txQuery = new pb.transaction.VertxTransactionQueryServiceGrpcClient(grpcClient, addrTransaction);
    var txCmd = new pb.transaction.VertxTransactionCommandServiceGrpcClient(grpcClient, addrTransaction);
    var txStatsAmt = new pb.transaction.stats.VertxTransactionStatsAmountServiceGrpcClient(grpcClient, addrTransaction);
    var txStatsMethod = new pb.transaction.stats.VertxTransactionStatsMethodServiceGrpcClient(grpcClient, addrTransaction);
    var txStatsStatus = new pb.transaction.stats.VertxTransactionStatsStatusServiceGrpcClient(grpcClient, addrTransaction);

    // 5. Setup Security Utilities
    JWTAuth jwtAuth = JwtConfig.createProvider(vertx);

    // 6. Instantiate high-performance Proxy Handlers
    var authHandler = new AuthProxyHandler(authClient);
    var userHandler = new UserProxyHandler(userQuery, userCmd);
    var roleHandler = new RoleProxyHandler(roleQuery, roleCmd);
    var saldoHandler = new SaldoProxyHandler(saldoQuery, saldoCmd, saldoStatsBal, saldoStatsTot);
    var cardHandler = new CardProxyHandler(cardQuery, cardCmd, cardDash, cardStatsBal, cardStatsTop, cardStatsWit, cardStatsTxn, cardStatsTrf);
    var merchantHandler = new MerchantProxyHandler(
        merchantQuery, merchantCmd, merchantDocCmd, merchantDocQuery,
        merchantStatsAmt, merchantStatsMet, merchantStatsTot, merchantTxn);
    var topupHandler = new TopupProxyHandler(topupQuery, topupCmd, topupStatsAmt, topupStatsMet, topupStatsSts);
    var transferHandler = new TransferProxyHandler(transferQuery, transferCmd, transferStatsAmt, transferStatsSts);
    var withdrawHandler = new WithdrawProxyHandler(withdrawQuery, withdrawCmd, withdrawStatsAmount, withdrawStatsStatus);
    var txHandler = new TransactionProxyHandler(txQuery, txCmd, txStatsAmt, txStatsMethod, txStatsStatus);

    // 7. Configure web routers & launch web interface
    Router baseRouter = Router.router(vertx);
    
    Router registeredRouter = GatewayRoutes.register(
        baseRouter,
        jwtAuth,
        merchantQuery,
        authHandler,
        userHandler,
        roleHandler,
        saldoHandler,
        cardHandler,
        merchantHandler,
        topupHandler,
        transferHandler,
        withdrawHandler,
        txHandler
    );

    int port = rawConfig.getInteger("http_port", 8080);
    String envHttpPort = System.getenv("HTTP_PORT");
    if (envHttpPort != null) port = Integer.parseInt(envHttpPort);

    final int finalPort = port;

    vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true))
        .requestHandler(registeredRouter)
        .listen(finalPort)
        .onSuccess(srv -> {
          log.info("====================================================================");
          log.info("🚀 API Gateway HUB successfully started at Port {}", finalPort);
          log.info("📈 OpenTelemetry instrumentation active for tracing.");
          log.info("====================================================================");
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("❌ CRITICAL: Failed to launch HTTP server for Gateway", err);
          startPromise.fail(err);
        });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (telemetryConfig != null) {
      telemetryConfig.shutdown();
    }
    if (grpcClient != null) {
      grpcClient.close();
    }
    stopPromise.complete();
  }

  private SocketAddress resolveGrpcAddress(String envPrefix, String defaultHost, int defaultPort) {
    String host = System.getenv().getOrDefault("GRPC_" + envPrefix + "_ADDR", defaultHost);
    int port = System.getenv("GRPC_" + envPrefix + "_PORT") != null 
        ? Integer.parseInt(System.getenv("GRPC_" + envPrefix + "_PORT")) 
        : defaultPort;
    
    // If ADDR contains a colon, split it
    if (host.contains(":")) {
      String[] parts = host.split(":");
      host = parts[0];
      port = Integer.parseInt(parts[1]);
    }
    
    log.info("📍 Service {} mapped to {}:{}", envPrefix, host, port);
    return SocketAddress.inetSocketAddress(port, host);
  }
}
