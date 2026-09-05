package io.example.apigateway.routes;

import io.example.apigateway.handler.*;
import io.example.apigateway.middleware.ApiKeyMiddleware;
import io.example.apigateway.middleware.JwtMiddleware;
import io.example.apigateway.middleware.RoleMiddleware;
import io.example.apigateway.utils.GrpcGatewayUtils;
import io.example.common.chaos.ChaosManager;
import io.example.common.observability.TracingMetrics;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;

public final class GatewayRoutes {
  private GatewayRoutes() {}

  public static Router register(
      Router router,
      JWTAuth jwtAuth,
      VertxMerchantQueryServiceGrpcClient merchantQueryClient,
      AuthProxyHandler auth,
      UserProxyHandler user,
      RoleProxyHandler role,
      SaldoProxyHandler saldo,
      CardProxyHandler card,
      MerchantProxyHandler merchant,
      TopupProxyHandler topup,
      TransferProxyHandler transfer,
      WithdrawProxyHandler withdraw,
      TransactionProxyHandler transaction,
      ChaosManager chaosManager,
      TracingMetrics tracingMetrics) {

    // 1. Global request metrics (registered first so every request is counted,
    // including ones rejected by later middleware/routes). Mirrors the
    // TracingMetrics usage in domain services: increments `requests_total` with
    // method + status attributes and records `request_duration_seconds`.
    router.route().handler(ctx -> {
      TracingMetrics.TracingContext tc = tracingMetrics.startSpan("gateway.request");
      ctx.response().endHandler(v -> {
        int status = ctx.response().getStatusCode();
        String path = ctx.request().path() != null ? ctx.request().path() : ctx.request().uri();
        String method = ctx.request().method().name() + " " + path;
        if (status >= 400) {
          tracingMetrics.completeSpanError(tc, method, "HTTP " + status);
        } else {
          tracingMetrics.completeSpanSuccess(tc, method, "HTTP " + status);
        }
      });
      ctx.next();
    });

    // 2. Global middleware (BodyParser is required for all JSON posts)
    router.route().handler(BodyHandler.create());

    // 2. Public / Health routes
    router.get("/health").handler(ctx -> ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(new JsonObject()
            .put("status", "UP")
            .put("service", "gateway")
            .encode()));

    // Kubernetes liveness probe — lightweight check that the server is alive
    router.get("/health/live").handler(ctx -> ctx.response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(200)
        .end(new JsonObject()
            .put("status", "alive")
            .encode()));

    // Kubernetes readiness probe — checks that the gateway can serve traffic
    router.get("/health/ready").handler(ctx -> ctx.response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(200)
        .end(new JsonObject()
            .put("status", "ready")
            .encode()));

    // =========================================================================
    // AUTH ROUTES (No API prefix in monolithic routes)
    // =========================================================================
    router.post("/register").handler(auth::register);
    router.post("/verify-code").handler(auth::verifyCode);
    router.post("/login").handler(auth::login);
    router.post("/refresh-token").handler(auth::refreshToken);
    router.get("/me").handler(JwtMiddleware.jwt(jwtAuth)).handler(auth::getMe);
    router.get("/logout").handler(JwtMiddleware.jwt(jwtAuth)).handler(auth::logout);

    // =========================================================================
    // USER ROUTES (JWT guarded, Admin only for list views)
    // =========================================================================
    router.route("/users*").handler(JwtMiddleware.jwt(jwtAuth));
    router.get("/users/active").handler(RoleMiddleware.requireRole("ROLE_ADMIN")).handler(user::findActive);
    router.get("/users/trashed").handler(RoleMiddleware.requireRole("ROLE_ADMIN")).handler(user::findTrashed);
    router.get("/users").handler(RoleMiddleware.requireRole("ROLE_ADMIN")).handler(user::findAll);
    router.get("/users/:id").handler(user::findById);
    router.post("/users/update/:id").handler(user::update);
    router.post("/users/restore/:id").handler(user::restore);
    router.post("/users/trashed/:id").handler(user::trashed);
    router.delete("/users/deletePermanent/:id").handler(user::deletePermanent);
    router.post("/users/restore-all").handler(user::restoreAllUsers);
    router.post("/users/delete-all").handler(user::deleteAllPermanentUsers);

    // =========================================================================
    // ROLE ROUTES (JWT guarded, Admin restricted)
    // =========================================================================
    router.route("/roles*").handler(JwtMiddleware.jwt(jwtAuth));
    router.get("/roles/active").handler(RoleMiddleware.requireRole("ROLE_ADMIN")).handler(role::findActive);
    router.get("/roles/trashed").handler(RoleMiddleware.requireRole("ROLE_ADMIN")).handler(role::findTrashed);
    router.get("/roles").handler(RoleMiddleware.requireRole("ROLE_ADMIN")).handler(role::findAll);
    router.get("/roles/:id").handler(role::findById);
    router.post("/roles").handler(role::create);
    router.post("/roles/:id").handler(role::update);
    router.post("/roles/restore/:id").handler(role::restore);
    router.post("/roles/trashed/:id").handler(role::trashed);
    router.delete("/roles/deletePermanent/:id").handler(role::deletePermanent);
    router.post("/roles/restore-all").handler(role::restoreAllRoles);
    router.post("/roles/delete-all").handler(role::deleteAllPermanentRoles);

    // =========================================================================
    // SALDO ROUTES (/api/saldos* prefix)
    // =========================================================================
    router.route("/api/saldos*").handler(JwtMiddleware.jwt(jwtAuth));
    router.get("/api/saldos").handler(saldo::getAllSaldos);
    router.get("/api/saldos/active").handler(saldo::getActiveSaldos);
    router.get("/api/saldos/trashed").handler(saldo::getTrashedSaldos);
    router.get("/api/saldos/:saldoId").handler(saldo::getSaldoById);
    router.get("/api/saldos/:cardNumber/monthly-total-balance").handler(saldo::getMonthlyTotalSaldoBalance);
    router.get("/api/saldos/stats/yearly-total-balances").handler(saldo::getYearlyTotalSaldoBalances);
    router.get("/api/saldos/stats/monthly-balances").handler(saldo::getMonthlySaldoBalances);
    router.get("/api/saldos/stats/yearly-balances").handler(saldo::getYearlySaldoBalances);
    router.post("/api/saldos").handler(saldo::createSaldo);
    router.put("/api/saldos").handler(saldo::updateSaldo);
    router.patch("/api/saldos/trash/:saldoId").handler(saldo::trashSaldo);
    router.patch("/api/saldos/restore/:saldoId").handler(saldo::restoreSaldo);
    router.delete("/api/saldos/permanent/:saldoId").handler(saldo::deleteSaldoPermanently);
    router.post("/api/saldos/restore-all").handler(saldo::restoreAllSaldos);
    router.delete("/api/saldos/permanent-all").handler(saldo::deleteAllPermanentSaldos);

    // =========================================================================
    // CARD ROUTES (/api/card* prefix)
    // =========================================================================
    router.route("/api/card*").handler(JwtMiddleware.jwt(jwtAuth));
    router.get("/api/card").handler(card::getAllCards);
    router.get("/api/card/active").handler(card::getActiveCards);
    router.get("/api/card/trashed").handler(card::getTrashedCards);
    router.post("/api/card/create").handler(card::createCard);
    router.post("/api/card/update/:cardId").handler(card::updateCard);
    router.post("/api/card/trashed/:cardId").handler(card::trashCard);
    router.post("/api/card/restore/:cardId").handler(card::restoreCard);
    router.delete("/api/card/permanent/:cardId").handler(card::deletePermanent);
    router.post("/api/card/restore-all").handler(card::restoreAllCards);
    router.post("/api/card/permanent-all").handler(card::deleteAllPermanentCards);
    
    // Card dashboard stats
    router.get("/api/card/dashboard").handler(card::getDashboard);
    router.get("/api/card/dashboard/:cardNumber").handler(card::getDashboardByCardNumber);
    router.get("/api/card/monthly-balance").handler(card::getMonthlyBalances);
    router.get("/api/card/yearly-balance").handler(card::getYearlyBalances);
    router.get("/api/card/monthly-topup-amount").handler(card::getMonthlyTopupAmount);
    router.get("/api/card/yearly-topup-amount").handler(card::getYearlyTopupAmount);
    router.get("/api/card/monthly-withdraw-amount").handler(card::getMonthlyWithdrawAmount);
    router.get("/api/card/yearly-withdraw-amount").handler(card::getYearlyWithdrawAmount);
    router.get("/api/card/monthly-transaction-amount").handler(card::getMonthlyTransactionAmount);
    router.get("/api/card/yearly-transaction-amount").handler(card::getYearlyTransactionAmount);
    router.get("/api/card/monthly-transfer-sender-amount").handler(card::getMonthlyTransferAmountSender);
    router.get("/api/card/yearly-transfer-sender-amount").handler(card::getYearlyTransferAmountSender);
    router.get("/api/card/monthly-transfer-receiver-amount").handler(card::getMonthlyTransferAmountReceiver);
    router.get("/api/card/yearly-transfer-receiver-amount").handler(card::getYearlyTransferAmountReceiver);
    // Card by number stats
    router.get("/api/card/monthly-balance-by-card").handler(card::getMonthlyBalancesByCardNumber);
    router.get("/api/card/yearly-balance-by-card").handler(card::getYearlyBalancesByCardNumber);
    router.get("/api/card/monthly-topup-amount-by-card").handler(card::getMonthlyTopupAmountByCardNumber);
    router.get("/api/card/yearly-topup-amount-by-card").handler(card::getYearlyTopupAmountByCardNumber);
    router.get("/api/card/monthly-withdraw-amount-by-card").handler(card::getMonthlyWithdrawAmountByCardNumber);
    router.get("/api/card/yearly-withdraw-amount-by-card").handler(card::getYearlyWithdrawAmountByCardNumber);
    router.get("/api/card/monthly-transaction-amount-by-card").handler(card::getMonthlyTransactionAmountByCardNumber);
    router.get("/api/card/yearly-transaction-amount-by-card").handler(card::getYearlyTransactionAmountByCardNumber);
    router.get("/api/card/monthly-transfer-sender-amount-by-card").handler(card::getMonthlyTransferAmountBySender);
    router.get("/api/card/yearly-transfer-sender-amount-by-card").handler(card::getYearlyTransferAmountBySender);
    router.get("/api/card/monthly-transfer-receiver-amount-by-card").handler(card::getMonthlyTransferAmountByReceiver);
    router.get("/api/card/yearly-transfer-receiver-amount-by-card").handler(card::getYearlyTransferAmountByReceiver);
    // KEEP AFTER the stats literal routes above: a Vert.x `:param` matches a single
    // path segment, so registering `:cardId` first would swallow /api/card/dashboard,
    // /api/card/monthly-balance, etc. as cardId="dashboard" -> 400. (regression guard)
    router.get("/api/card/:cardId").handler(card::getCardById);

    // =========================================================================
    // CREDIT CARD LIFECYCLE ROUTES (/api/v1/cards* prefix)
    // =========================================================================
    router.route("/api/v1/cards*").handler(JwtMiddleware.jwt(jwtAuth));
    router.post("/api/v1/cards/authorize").handler(card::handleAuthorize);
    router.post("/api/v1/cards/reverse").handler(card::handleReversal);
    router.post("/api/v1/cards/payment").handler(card::handlePostPayment);
    router.get("/api/v1/cards/:cardNumber/statement").handler(card::handleGetStatement);
    router.get("/api/v1/cards/:cardNumber/statements").handler(card::handleGetStatements);
    router.post("/api/v1/cards/billing/trigger").handler(card::handleTriggerBilling);
    router.get("/api/v1/cards/:cardNumber/payments").handler(card::handlePaymentHistory);
    router.get("/api/v1/cards/:cardNumber/limit").handler(card::handleGetLimit);
    router.post("/api/v1/cards/:cardNumber/limit").handler(card::handleSetLimit);
    router.get("/api/v1/cards/:cardNumber/rewards").handler(card::handleGetRewards);

    // =========================================================================
    // MERCHANT ROUTES (/api/merchants* prefix)
    // =========================================================================
    router.route("/api/merchants*").handler(JwtMiddleware.jwt(jwtAuth));
    router.get("/api/merchants").handler(merchant::getAllMerchants);
    router.get("/api/merchants/active").handler(merchant::getActiveMerchants);
    router.get("/api/merchants/trashed").handler(merchant::getTrashedMerchants);
    router.get("/api/merchants/api-key/:apiKey").handler(merchant::getMerchantByApiKey);
    router.get("/api/merchants/by-name").handler(merchant::getMerchantByName);
    router.get("/api/merchants/by-user/:userId").handler(merchant::getMerchantsByUserId);
    router.post("/api/merchants").handler(merchant::createMerchant);
    router.put("/api/merchants").handler(merchant::updateMerchant);
    router.patch("/api/merchants/status").handler(merchant::updateMerchantStatus);
    router.patch("/api/merchants/trash/:merchantId").handler(merchant::trashMerchant);
    router.patch("/api/merchants/restore/:merchantId").handler(merchant::restoreMerchant);
    router.delete("/api/merchants/permanent/:merchantId").handler(merchant::deleteMerchantPermanently);
    router.post("/api/merchants/restore-all").handler(merchant::restoreAllMerchants);
    router.delete("/api/merchants/delete-all").handler(merchant::deleteAllPermanentMerchants);
    // Merchant analytics
    router.get("/api/merchants/transactions").handler(merchant::findAllTransactions);
    router.get("/api/merchants/transactions/api-key/:apiKey").handler(merchant::findAllTransactionsByApiKey);
    router.get("/api/merchants/transactions/:merchantId").handler(merchant::findAllTransactionsByMerchantId);
    router.get("/api/merchants/monthly-payment-methods").handler(merchant::getMonthlyPaymentMethodsMerchant);
    router.get("/api/merchants/yearly-payment-methods").handler(merchant::getYearlyPaymentMethodMerchant);
    router.get("/api/merchants/monthly-amount").handler(merchant::getMonthlyAmountMerchant);
    router.get("/api/merchants/yearly-amount").handler(merchant::getYearlyAmountMerchant);
    router.get("/api/merchants/monthly-total-amount").handler(merchant::getMonthlyTotalAmountMerchant);
    router.get("/api/merchants/yearly-total-amount").handler(merchant::getYearlyTotalAmountMerchant);
    // Merchant Analytics by ID
    router.get("/api/merchants/monthly-payment-methods-by-merchant/:merchantId").handler(merchant::getMonthlyPaymentMethodByMerchant);
    router.get("/api/merchants/yearly-payment-methods-by-merchant/:merchantId").handler(merchant::getYearlyPaymentMethodByMerchants);
    router.get("/api/merchants/monthly-amount-by-merchant/:merchantId").handler(merchant::getMonthlyAmountByMerchants);
    router.get("/api/merchants/yearly-amount-by-merchant/:merchantId").handler(merchant::getYearlyAmountByMerchants);
    router.get("/api/merchants/monthly-totalamount-by-merchant/:merchantId").handler(merchant::getMonthlyTotalAmountByMerchant);
    router.get("/api/merchants/yearly-totalamount-by-merchant/:merchantId").handler(merchant::getYearlyTotalAmountByMerchant);
    // Merchant Analytics by API KEY
    router.get("/api/merchants/monthly-payment-methods-by-apikey/:apiKey").handler(merchant::getMonthlyPaymentMethodByApiKey);
    router.get("/api/merchants/yearly-payment-methods-by-apikey/:apiKey").handler(merchant::getYearlyPaymentMethodByApiKey);
    router.get("/api/merchants/monthly-amount-by-apikey/:apiKey").handler(merchant::getMonthlyAmountByApiKey);
    router.get("/api/merchants/yearly-amount-by-apikey/:apiKey").handler(merchant::getYearlyAmountByApiKey);
    router.get("/api/merchants/monthly-totalamount-by-apikey/:apiKey").handler(merchant::getMonthlyTotalAmountByApiKey);
    router.get("/api/merchants/yearly-totalamount-by-apikey/:apiKey").handler(merchant::getYearlyTotalAmountByApiKey);
    // KEEP AFTER the analytics literal routes (see :cardId guard above)
    router.get("/api/merchants/:merchantId").handler(merchant::getMerchantById);

    // =========================================================================
    // MERCHANT DOCUMENTS (/api/merchant-documents* prefix)
    // =========================================================================
    router.route("/api/merchant-documents*").handler(JwtMiddleware.jwt(jwtAuth));
    router.get("/api/merchant-documents").handler(merchant::getAllMerchantDocuments);
    router.get("/api/merchant-documents/active").handler(merchant::getActiveMerchantDocuments);
    router.get("/api/merchant-documents/trashed").handler(merchant::getTrashedMerchantDocuments);
    router.get("/api/merchant-documents/:documentId").handler(merchant::getMerchantDocumentById);
    router.post("/api/merchant-documents").handler(merchant::createMerchantDocument);
    router.put("/api/merchant-documents/:documentId").handler(merchant::updateMerchantDocument);
    router.patch("/api/merchant-documents/:documentId/status").handler(merchant::updateMerchantDocumentStatus);
    router.patch("/api/merchant-documents/:documentId/trash").handler(merchant::trashMerchantDocument);
    router.patch("/api/merchant-documents/:documentId/restore").handler(merchant::restoreMerchantDocument);
    router.delete("/api/merchant-documents/:documentId/permanent").handler(merchant::deleteMerchantDocumentPermanently);
    router.patch("/api/merchant-documents/restore-all").handler(merchant::restoreAllMerchantDocuments);
    router.delete("/api/merchant-documents/permanent-all").handler(merchant::deleteAllPermanentMerchantDocuments);

    // =========================================================================
    // TOPUP ROUTES (/api/topups* prefix)
    // =========================================================================
    router.route("/api/topups*").handler(JwtMiddleware.jwt(jwtAuth));
    router.get("/api/topups").handler(topup::getTopups);
    router.get("/api/topups/active").handler(topup::getActiveTopups);
    router.get("/api/topups/trashed").handler(topup::getTrashedTopups);
    router.post("/api/topups/create").handler(topup::createTopup);
    router.post("/api/topups/update").handler(topup::updateTopup);
    router.post("/api/topups/trash/:topupId").handler(topup::trashTopup);
    router.post("/api/topups/restore/:topupId").handler(topup::restoreTopup);
    router.delete("/api/topups/permanent/:topupId").handler(topup::deleteTopupPermanently);
    // Backward-compatible alias for clients using the historical typo.
    router.delete("/api/topups/permenent/:topupId").handler(topup::deleteTopupPermanently);
    router.post("/api/topups/restore-all").handler(topup::restoreAllTopups);
    router.delete("/api/topups/permanent-all").handler(topup::deleteAllPermanentTopups);
    // Topup stats
    router.get("/api/topups/monthly-success").handler(topup::getMonthTopupStatusSuccess);
    router.get("/api/topups/yearly-success").handler(topup::getYearlyTopupStatusSuccess);
    router.get("/api/topups/monthly-failed").handler(topup::getMonthTopupStatusFailed);
    router.get("/api/topups/yearly-failed").handler(topup::getYearlyTopupStatusFailed);
    router.get("/api/topups/monthly-methods").handler(topup::getMonthlyTopupMethods);
    router.get("/api/topups/yearly-methods").handler(topup::getYearlyTopupMethods);
    router.get("/api/topups/monthly-amounts").handler(topup::getMonthlyTopupAmounts);
    router.get("/api/topups/yearly-amounts").handler(topup::getYearlyTopupAmounts);
    // Topup stats by card
    router.get("/api/topups/monthly-success-by-card/:cardNumber").handler(topup::getMonthTopupStatusSuccessCardNumber);
    router.get("/api/topups/yearly-success-by-card/:cardNumber").handler(topup::getYearlyTopupStatusSuccessCardNumber);
    router.get("/api/topups/monthly-failed-by-card/:cardNumber").handler(topup::getMonthTopupStatusFailedCardNumber);
    router.get("/api/topups/yearly-failed-by-card/:cardNumber").handler(topup::getYearlyTopupStatusFailedCardNumber);
    router.get("/api/topups/monthly-methods-by-card/:cardNumber").handler(topup::getMonthlyTopupMethodsByCardNumber);
    router.get("/api/topups/yearly-methods-by-card/:cardNumber").handler(topup::getYearlyTopupMethodsByCardNumber);
    router.get("/api/topups/monthly-amounts-by-card/:cardNumber").handler(topup::getMonthlyTopupAmountsByCardNumber);
    router.get("/api/topups/yearly-amounts-by-card/:cardNumber").handler(topup::getYearlyTopupAmountsByCardNumber);
    // KEEP AFTER the stats literal routes (see :cardId guard above)
    router.get("/api/topups/:topupId").handler(topup::getTopupById);

    // =========================================================================
    // TRANSFER ROUTES (/api/transfers* prefix)
    // =========================================================================
    router.route("/api/transfers*").handler(JwtMiddleware.jwt(jwtAuth));
    router.get("/api/transfers").handler(transfer::getAllTransfers);
    router.get("/api/transfers/active").handler(transfer::getActiveTransfers);
    router.get("/api/transfers/trashed").handler(transfer::getTrashedTransfers);
    router.get("/api/transfers/by-card/:cardNumber").handler(transfer::getTransfersByCardNumber);
    router.get("/api/transfers/transfer_from/:cardNumber").handler(transfer::getTransfersAsSender);
    router.get("/api/transfers/transfer_to/:cardNumber").handler(transfer::getTransfersAsReceiver);
    router.post("/api/transfers/create").handler(transfer::createTransfer);
    router.post("/api/transfers/update").handler(transfer::updateTransfer);
    router.post("/api/transfers/trash/:transferId").handler(transfer::trashTransfer);
    router.post("/api/transfers/restore/:transferId").handler(transfer::restoreTransfer);
    router.delete("/api/transfers/permanent/:transferId").handler(transfer::deleteTransferPermanent);
    router.post("/api/transfers/restore-all").handler(transfer::restoreAllTransfers);
    router.delete("/api/transfers/permanent-all").handler(transfer::deleteAllPermanentTransfers);
    // Transfer stats
    router.get("/api/transfers/monthly-success").handler(transfer::getMonthTransferStatusSuccess);
    router.get("/api/transfers/yearly-success").handler(transfer::getYearlyTransferStatusSuccess);
    router.get("/api/transfers/monthly-failed").handler(transfer::getMonthTransferStatusFailed);
    router.get("/api/transfers/yearly-failed").handler(transfer::getYearlyTransferStatusFailed);
    router.get("/api/transfers/monthly-amount").handler(transfer::getMonthlyTransferAmounts);
    router.get("/api/transfers/yearly-amount").handler(transfer::getYearlyTransferAmounts);
    // Transfer stats by card
    router.get("/api/transfers/monthly-success-by-card/:cardNumber").handler(transfer::getMonthTransferStatusSuccessCardNumber);
    router.get("/api/transfers/yearly-success-by-card/:cardNumber").handler(transfer::getYearlyTransferStatusSuccessCardNumber);
    router.get("/api/transfers/monthly-failed-by-card/:cardNumber").handler(transfer::getMonthTransferStatusFailedCardNumber);
    router.get("/api/transfers/yearly-failed-by-card/:cardNumber").handler(transfer::getYearlyTransferStatusFailedCardNumber);
    router.get("/api/transfers/monthly-by-sender/:cardNumber").handler(transfer::getMonthlyTransferAmountsBySenderCardNumber);
    router.get("/api/transfers/yearly-by-sender/:cardNumber").handler(transfer::getYearlyTransferAmountsBySenderCardNumber);
    router.get("/api/transfers/monthly-by-receiver/:cardNumber").handler(transfer::getMonthlyTransferAmountsByReceiverCardNumber);
    router.get("/api/transfers/yearly-by-receiver/:cardNumber").handler(transfer::getYearlyTransferAmountsByReceiverCardNumber);
    // KEEP AFTER the stats literal routes (see :cardId guard above)
    router.get("/api/transfers/:transferId").handler(transfer::getTransferById);

    // =========================================================================
    // WITHDRAW ROUTES (/api/withdraws* prefix)
    // =========================================================================
    router.route("/api/withdraws*").handler(JwtMiddleware.jwt(jwtAuth));
    router.get("/api/withdraws").handler(withdraw::getAllWithdraws);
    router.get("/api/withdraws/active").handler(withdraw::getActiveWithdraws);
    router.get("/api/withdraws/trashed").handler(withdraw::getTrashedWithdraws);
    router.post("/api/withdraws/create").handler(withdraw::createWithdraw);
    router.post("/api/withdraws/update").handler(withdraw::updateWithdraw);
    router.post("/api/withdraws/trash/:withdrawId").handler(withdraw::trash);
    router.post("/api/withdraws/restore/:withdrawId").handler(withdraw::restore);
    router.post("/api/withdraws/permanent/:withdrawId").handler(withdraw::deletePermanent);
    router.post("/api/withdraws/restore-all").handler(withdraw::restoreAllWithdraws);
    router.delete("/api/withdraws/permanent-all").handler(withdraw::deleteAllPermanentWithdraws);
    // Withdraw stats
    router.get("/api/withdraws/monthly-success").handler(withdraw::getMonthWithdrawStatusSuccess);
    router.get("/api/withdraws/yearly-success").handler(withdraw::getYearlyWithdrawStatusSuccess);
    router.get("/api/withdraws/monthly-failed").handler(withdraw::getMonthWithdrawStatusFailed);
    router.get("/api/withdraws/yearly-failed").handler(withdraw::getYearlyWithdrawStatusFailed);
    router.get("/api/withdraws/monthly-amount").handler(withdraw::getMonthlyWithdraws);
    router.get("/api/withdraws/yearly-amount").handler(withdraw::getYearlyWithdraws);
    // Withdraw stats by card
    router.get("/api/withdraws/monthly-success-by-card/:cardNumber").handler(withdraw::getMonthWithdrawStatusSuccessCardNumber);
    router.get("/api/withdraws/yearly-success-by-card/:cardNumber").handler(withdraw::getYearlyWithdrawStatusSuccessCardNumber);
    router.get("/api/withdraws/monthly-failed-by-card/:cardNumber").handler(withdraw::getMonthWithdrawStatusFailedCardNumber);
    router.get("/api/withdraws/yearly-failed-by-card/:cardNumber").handler(withdraw::getYearlyWithdrawStatusFailedCardNumber);
    router.get("/api/withdraws/monthly-amount-bycard/:cardNumber").handler(withdraw::getMonthlyWithdrawsByCardNumber);
    router.get("/api/withdraws/yearly-amount-bycard/:cardNumber").handler(withdraw::getYearlyWithdrawsByCardNumber);
    // KEEP AFTER the stats literal routes (see :cardId guard above)
    router.get("/api/withdraws/:withdrawId").handler(withdraw::getWithdrawById);

    // =========================================================================
    // TRANSACTION ROUTES (/transactions* prefix)
    // =========================================================================
    // Merchant API-KEY routes MUST be registered before the broad JWT-guarded
    // /transactions* route: they authenticate with X-Api-Key only (no Bearer
    // token) and would otherwise be rejected as Unauthorized by JwtMiddleware.
    router.post("/transactions/create")
        .handler(ApiKeyMiddleware.requireApiKey(merchantQueryClient))
        .handler(transaction::createTransaction);

    router.post("/transactions/update")
        .handler(ApiKeyMiddleware.requireApiKey(merchantQueryClient))
        .handler(transaction::updateTransaction);

    router.route("/transactions*").handler(JwtMiddleware.jwt(jwtAuth));
    router.get("/transactions").handler(transaction::getTransactions);
    router.get("/transactions/active").handler(transaction::getActiveTransactions);
    router.get("/transactions/trashed").handler(transaction::getTrashedTransactions);
    router.get("/transactions/by-card/:cardNumber").handler(transaction::getTransactionsByCardNumber);
    // Transaction stats
    router.get("/transactions/monthly-success").handler(transaction::getMonthTransactionStatusSuccess);
    router.get("/transactions/yearly-success").handler(transaction::getYearlyTransactionStatusSuccess);
    router.get("/transactions/monthly-failed").handler(transaction::getMonthTransactionStatusFailed);
    router.get("/transactions/yearly-failed").handler(transaction::getYearlyTransactionStatusFailed);
    router.get("/transactions/monthly-methods").handler(transaction::getMonthlyPaymentMethods);
    router.get("/transactions/yearly-methods").handler(transaction::getYearlyPaymentMethods);
    router.get("/transactions/monthly-amounts").handler(transaction::getMonthlyAmounts);
    router.get("/transactions/yearly-amounts").handler(transaction::getYearlyAmounts);
    // Transaction stats by card
    router.get("/transactions/monthly-success-by-card/:cardNumber").handler(transaction::getMonthTransactionStatusSuccessCardNumber);
    router.get("/transactions/yearly-success-by-card/:cardNumber").handler(transaction::getYearlyTransactionStatusSuccessCardNumber);
    router.get("/transactions/monthly-failed-by-card/:cardNumber").handler(transaction::getMonthTransactionStatusFailedCardNumber);
    router.get("/transactions/yearly-failed-by-card/:cardNumber").handler(transaction::getYearlyTransactionStatusFailedCardNumber);
    router.get("/transactions/monthly-methods-by-card/:cardNumber").handler(transaction::getMonthlyPaymentMethodsByCardNumber);
    router.get("/transactions/yearly-methods-by-card/:cardNumber").handler(transaction::getYearlyPaymentMethodsByCardNumber);
    router.get("/transactions/monthly-amounts-by-card/:cardNumber").handler(transaction::getMonthlyAmountsByCardNumber);
    router.get("/transactions/yearly-amounts-by-card/:cardNumber").handler(transaction::getYearlyAmountsByCardNumber);
    // KEEP AFTER the stats literal routes (see :cardId guard above)
    router.get("/transactions/:transactionId").handler(transaction::getTransactionById);
    
    // Lifecycle commands
    router.post("/transactions/trash/:transactionId").handler(transaction::trashTransaction);
    router.post("/transactions/restore/:transactionId").handler(transaction::restoreTransaction);
    router.delete("/transactions/permanent/:transactionId").handler(transaction::deleteTransactionPermanently);
    // Backward-compatible alias for clients using the historical typo.
    router.delete("/transactions/permanenet/:transactionId").handler(transaction::deleteTransactionPermanently);
    router.post("/transactions/restore-all").handler(transaction::restoreAllTransactions);
    router.delete("/transactions/permanent-all").handler(transaction::deleteAllPermanentTransactions);

    // =========================================================================
    // CHAOS CONTROL PLANE ROUTES
    // Vert.x forbids adding an AUTHENTICATION handler (JWT) to a route whose first
    // handler is a plain USER handler, so the disabled check and the protected
    // handler live on two separate routes. When CHAOS_ENABLED=false the first route
    // answers 404 without touching auth (default production behavior); when enabled
    // it delegates to the protected route via next().
    // =========================================================================
    router.get("/api/chaos/policies").handler(ctx -> {
      if (!chaosManager.isEnabled()) {
        ctx.response().setStatusCode(404).end();
        return;
      }
      ctx.next();
    });
    router.get("/api/chaos/policies")
        .handler(JwtMiddleware.jwt(jwtAuth))
        .handler(RoleMiddleware.requireRole("ROLE_ADMIN"))
        .handler(ctx -> {
      JsonArray policiesArr = new JsonArray();
      chaosManager.getPolicies().forEach(policy -> {
        policiesArr.add(JsonObject.mapFrom(policy));
      });
      ctx.response()
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("policies", policiesArr).encodePrettily());
    });

    router.post("/api/chaos/halt").handler(ctx -> {
      if (!chaosManager.isEnabled()) {
        ctx.response().setStatusCode(404).end();
        return;
      }
      ctx.next();
    });
    router.post("/api/chaos/halt")
        .handler(JwtMiddleware.jwt(jwtAuth))
        .handler(RoleMiddleware.requireRole("ROLE_ADMIN"))
        .handler(ctx -> {
      chaosManager.halt();
      ctx.response()
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("status", "success").put("message", "All chaos experiments halted").encodePrettily());
    });

    router.post("/api/chaos/policies/reload").handler(ctx -> {
      if (!chaosManager.isEnabled()) {
        ctx.response().setStatusCode(404).end();
        return;
      }
      ctx.next();
    });
    router.post("/api/chaos/policies/reload")
        .handler(JwtMiddleware.jwt(jwtAuth))
        .handler(RoleMiddleware.requireRole("ROLE_ADMIN"))
        .handler(ctx -> {
      chaosManager.loadConfig();
      ctx.response()
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("status", "success").put("message", "Chaos policies reloaded").encodePrettily());
    });

    // =========================================================================
    // GLOBAL FAILURE HANDLER (must be registered last)
    // Maps synchronous handler exceptions (ApiException validation, invalid path
    // params) to proper HTTP status codes instead of a generic 500.
    // =========================================================================
    router.route().failureHandler(GrpcGatewayUtils::handleRouteFailure);

    return router;
  }
}
