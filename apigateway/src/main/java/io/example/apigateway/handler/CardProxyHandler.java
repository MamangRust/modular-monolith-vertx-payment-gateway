package io.example.apigateway.handler;

import java.time.Instant;
import java.time.OffsetDateTime;

import com.google.protobuf.util.Timestamps;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import pb.card.Card;
import pb.card.CardAuthorization;
import pb.card.CardBilling;
import pb.card.CardCommand;
import pb.card.CardLimit;
import pb.card.CardPayment;
import pb.card.VertxCardAuthorizationServiceGrpcClient;
import pb.card.VertxCardBillingServiceGrpcClient;
import pb.card.VertxCardCommandServiceGrpcClient;
import pb.card.VertxCardDashboardServiceGrpcClient;
import pb.card.VertxCardLimitServiceGrpcClient;
import pb.card.VertxCardPaymentServiceGrpcClient;
import pb.card.VertxCardQueryServiceGrpcClient;
import pb.card.stats.VertxCardStatsBalanceServiceGrpcClient;
import pb.card.stats.VertxCardStatsTopupServiceGrpcClient;
import pb.card.stats.VertxCardStatsTransactionServiceGrpcClient;
import pb.card.stats.VertxCardStatsTransferServiceGrpcClient;
import pb.card.stats.VertxCardStatsWithdrawServiceGrpcClient;

@RequiredArgsConstructor
public class CardProxyHandler {
  private final VertxCardQueryServiceGrpcClient queryClient;
  private final VertxCardCommandServiceGrpcClient commandClient;
  private final VertxCardDashboardServiceGrpcClient dashboardClient;
  private final VertxCardStatsBalanceServiceGrpcClient balanceClient;
  private final VertxCardStatsTopupServiceGrpcClient topupClient;
  private final VertxCardStatsWithdrawServiceGrpcClient withdrawClient;
  private final VertxCardStatsTransactionServiceGrpcClient transactionClient;
  private final VertxCardStatsTransferServiceGrpcClient transferClient;

  // Credit lifecycle clients
  private final VertxCardAuthorizationServiceGrpcClient authClient;
  private final VertxCardPaymentServiceGrpcClient paymentClient;
  private final VertxCardBillingServiceGrpcClient billingClient;
  private final VertxCardLimitServiceGrpcClient limitClient;

  private Card.FindAllCardRequest buildFindAllReq(RoutingContext ctx) {
    return Card.FindAllCardRequest.newBuilder()
        .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
        .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
        .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
        .build();
  }

  private Card.FindYearAmount buildFindYearAmountReq(RoutingContext ctx) {
    return Card.FindYearAmount.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .build();
  }

  private Card.FindYearAmountCardNumber buildYearAmountCardReq(RoutingContext ctx) {
    return Card.FindYearAmountCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setCardNumber(GrpcGatewayUtils.getQueryString(ctx, "card_number", ""))
        .build();
  }

  private pb.card.stats.CardStatsBalance.FindYearBalanceCardNumber buildYearBalanceCardReq(RoutingContext ctx) {
    return pb.card.stats.CardStatsBalance.FindYearBalanceCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setCardNumber(GrpcGatewayUtils.getQueryString(ctx, "card_number", ""))
        .build();
  }

  public void getAllCards(RoutingContext ctx) {
    queryClient.findAllCard(buildFindAllReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getActiveCards(RoutingContext ctx) {
    queryClient.findByActiveCard(buildFindAllReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTrashedCards(RoutingContext ctx) {
    queryClient.findByTrashedCard(buildFindAllReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getCardById(RoutingContext ctx) {
    var req = Card.FindByIdCardRequest.newBuilder()
        .setCardId(GrpcGatewayUtils.getSafePathInt(ctx, "cardId"))
        .build();
    queryClient.findByIdCard(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void createCard(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = CardCommand.CreateCardRequest.newBuilder()
        .setUserId(GrpcGatewayUtils.getJsonInteger(body, "user_id", 0))
        .setCardType(GrpcGatewayUtils.getJsonString(body, "card_type", ""))
        .setExpireDate(toTimestamp(GrpcGatewayUtils.getJsonString(body, "expiry_date", "")))
        .setCvv(GrpcGatewayUtils.getJsonString(body, "cvv", ""))
        .setCardProvider(GrpcGatewayUtils.getJsonString(body, "card_provider", ""))
        .build();
    commandClient.createCard(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 201))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void updateCard(RoutingContext ctx) {
    int id = GrpcGatewayUtils.getSafePathInt(ctx, "cardId");
    JsonObject body = ctx.body().asJsonObject();
    var req = CardCommand.UpdateCardRequest.newBuilder()
        .setCardId(id)
        .setUserId(GrpcGatewayUtils.getJsonInteger(body, "user_id", 0))
        .setCardType(GrpcGatewayUtils.getJsonString(body, "card_type", ""))
        .setExpireDate(toTimestamp(GrpcGatewayUtils.getJsonString(body, "expiry_date", "")))
        .setCvv(GrpcGatewayUtils.getJsonString(body, "cvv", ""))
        .setCardProvider(GrpcGatewayUtils.getJsonString(body, "card_provider", ""))
        .build();
    commandClient.updateCard(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void trashCard(RoutingContext ctx) {
    var req = Card.FindByIdCardRequest.newBuilder()
        .setCardId(GrpcGatewayUtils.getSafePathInt(ctx, "cardId"))
        .build();
    commandClient.trashedCard(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreCard(RoutingContext ctx) {
    var req = Card.FindByIdCardRequest.newBuilder()
        .setCardId(GrpcGatewayUtils.getSafePathInt(ctx, "cardId"))
        .build();
    commandClient.restoreCard(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deletePermanent(RoutingContext ctx) {
    var req = Card.FindByIdCardRequest.newBuilder()
        .setCardId(GrpcGatewayUtils.getSafePathInt(ctx, "cardId"))
        .build();
    commandClient.deleteCardPermanent(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreAllCards(RoutingContext ctx) {
    commandClient.restoreAllCard(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteAllPermanentCards(RoutingContext ctx) {
    commandClient.deleteAllCardPermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getDashboard(RoutingContext ctx) {
    dashboardClient.dashboardCard(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getDashboardByCardNumber(RoutingContext ctx) {
    String num = ctx.pathParam("cardNumber");
    var req = Card.FindByCardNumberRequest.newBuilder().setCardNumber(num).build();
    dashboardClient.dashboardCardNumber(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyBalances(RoutingContext ctx) {
    var req = pb.card.stats.CardStatsBalance.FindYearBalance.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024)).build();
    balanceClient.findMonthlyBalance(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyBalances(RoutingContext ctx) {
    var req = pb.card.stats.CardStatsBalance.FindYearBalance.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024)).build();
    balanceClient.findYearlyBalance(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTopupAmount(RoutingContext ctx) {
    topupClient.findMonthlyTopupAmount(buildFindYearAmountReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTopupAmount(RoutingContext ctx) {
    topupClient.findYearlyTopupAmount(buildFindYearAmountReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyWithdrawAmount(RoutingContext ctx) {
    withdrawClient.findMonthlyWithdrawAmount(buildFindYearAmountReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyWithdrawAmount(RoutingContext ctx) {
    withdrawClient.findYearlyWithdrawAmount(buildFindYearAmountReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTransactionAmount(RoutingContext ctx) {
    transactionClient.findMonthlyTransactionAmount(buildFindYearAmountReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransactionAmount(RoutingContext ctx) {
    transactionClient.findYearlyTransactionAmount(buildFindYearAmountReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTransferAmountSender(RoutingContext ctx) {
    transferClient.findMonthlyTransferSenderAmount(buildFindYearAmountReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransferAmountSender(RoutingContext ctx) {
    transferClient.findYearlyTransferSenderAmount(buildFindYearAmountReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTransferAmountReceiver(RoutingContext ctx) {
    transferClient.findMonthlyTransferReceiverAmount(buildFindYearAmountReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransferAmountReceiver(RoutingContext ctx) {
    transferClient.findYearlyTransferReceiverAmount(buildFindYearAmountReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyBalancesByCardNumber(RoutingContext ctx) {
    balanceClient.findMonthlyBalanceByCardNumber(buildYearBalanceCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyBalancesByCardNumber(RoutingContext ctx) {
    balanceClient.findYearlyBalanceByCardNumber(buildYearBalanceCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTopupAmountByCardNumber(RoutingContext ctx) {
    topupClient.findMonthlyTopupAmountByCardNumber(buildYearAmountCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTopupAmountByCardNumber(RoutingContext ctx) {
    topupClient.findYearlyTopupAmountByCardNumber(buildYearAmountCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyWithdrawAmountByCardNumber(RoutingContext ctx) {
    withdrawClient.findMonthlyWithdrawAmountByCardNumber(buildYearAmountCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyWithdrawAmountByCardNumber(RoutingContext ctx) {
    withdrawClient.findYearlyWithdrawAmountByCardNumber(buildYearAmountCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTransactionAmountByCardNumber(RoutingContext ctx) {
    transactionClient.findMonthlyTransactionAmountByCardNumber(buildYearAmountCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransactionAmountByCardNumber(RoutingContext ctx) {
    transactionClient.findYearlyTransactionAmountByCardNumber(buildYearAmountCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTransferAmountBySender(RoutingContext ctx) {
    transferClient.findMonthlyTransferSenderAmountByCardNumber(buildYearAmountCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransferAmountBySender(RoutingContext ctx) {
    transferClient.findYearlyTransferSenderAmountByCardNumber(buildYearAmountCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTransferAmountByReceiver(RoutingContext ctx) {
    transferClient.findMonthlyTransferReceiverAmountByCardNumber(buildYearAmountCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransferAmountByReceiver(RoutingContext ctx) {
    transferClient.findYearlyTransferReceiverAmountByCardNumber(buildYearAmountCardReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  // =========================================================================
  // CREDIT CARD LIFECYCLE ENDPOINTS
  // =========================================================================

  public void handleAuthorize(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = CardAuthorization.AuthorizeRequest.newBuilder()
        .setCardNumber(GrpcGatewayUtils.getJsonString(body, "card_number", ""))
        .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
        .setAmount(GrpcGatewayUtils.getJsonLong(body, "amount", 0L))
        .setCurrency(GrpcGatewayUtils.getJsonString(body, "currency", "IDR"))
        .setPosEntryMode(GrpcGatewayUtils.getJsonString(body, "pos_entry_mode", ""))
        .setMcc(GrpcGatewayUtils.getJsonString(body, "mcc", ""))
        .setIdempotencyKey(GrpcGatewayUtils.getJsonString(body, "idempotency_key", ""))
        .build();
    authClient.authorize(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void handleReversal(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = CardAuthorization.ReverseRequest.newBuilder()
        .setTxnId(GrpcGatewayUtils.getJsonString(body, "txn_id", ""))
        .setCardNumber(GrpcGatewayUtils.getJsonString(body, "card_number", ""))
        .setAmount(GrpcGatewayUtils.getJsonLong(body, "amount", 0L))
        .setIdempotencyKey(GrpcGatewayUtils.getJsonString(body, "idempotency_key", ""))
        .build();
    authClient.reverse(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void handlePostPayment(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = CardPayment.PostPaymentRequest.newBuilder()
        .setReferenceId(GrpcGatewayUtils.getJsonString(body, "reference_id", ""))
        .setCardNumber(GrpcGatewayUtils.getJsonString(body, "card_number", ""))
        .setAmount(GrpcGatewayUtils.getJsonLong(body, "amount", 0L))
        .setPaymentChannel(GrpcGatewayUtils.getJsonString(body, "payment_channel", ""))
        .setStatementId(GrpcGatewayUtils.getJsonInteger(body, "statement_id", 0))
        .build();
    paymentClient.postPayment(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void handlePaymentHistory(RoutingContext ctx) {
    String cardNumber = ctx.pathParam("cardNumber");
    int page = GrpcGatewayUtils.getQueryInt(ctx, "page", 1);
    int pageSize = GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10);
    var req = CardPayment.GetPaymentHistoryRequest.newBuilder()
        .setCardNumber(cardNumber)
        .setPage(page)
        .setPageSize(pageSize)
        .build();
    paymentClient.getPaymentHistory(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void handleGetStatement(RoutingContext ctx) {
    String cardNumber = ctx.pathParam("cardNumber");
    String statementDate = GrpcGatewayUtils.getQueryString(ctx, "statement_date", "");
    var req = CardBilling.GetStatementRequest.newBuilder()
        .setCardNumber(cardNumber)
        .setStatementDate(statementDate)
        .build();
    billingClient.getStatement(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void handleGetStatements(RoutingContext ctx) {
    String cardNumber = ctx.pathParam("cardNumber");
    int page = GrpcGatewayUtils.getQueryInt(ctx, "page", 1);
    int pageSize = GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10);
    var req = CardBilling.GetStatementsByCardRequest.newBuilder()
        .setCardNumber(cardNumber)
        .setPage(page)
        .setPageSize(pageSize)
        .build();
    billingClient.getStatementsByCard(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void handleTriggerBilling(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = CardBilling.TriggerBillingRequest.newBuilder()
        .setBillingCycleDay(GrpcGatewayUtils.getJsonInteger(body, "billing_cycle_day", 1))
        .build();
    billingClient.triggerBillingCycle(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void handleGetLimit(RoutingContext ctx) {
    String cardNumber = ctx.pathParam("cardNumber");
    var req = CardLimit.GetLimitByCardNumberRequest.newBuilder()
        .setCardNumber(cardNumber)
        .build();
    limitClient.getLimit(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void handleSetLimit(RoutingContext ctx) {
    String cardNumber = ctx.pathParam("cardNumber");
    JsonObject body = ctx.body().asJsonObject();
    var req = CardLimit.SetLimitRequest.newBuilder()
        .setCardNumber(cardNumber)
        .setCreditLimit(GrpcGatewayUtils.getJsonLong(body, "credit_limit", 0L))
        .setBillingCycleDay(GrpcGatewayUtils.getJsonInteger(body, "billing_cycle_day", 1))
        .setAnnualRateBps(GrpcGatewayUtils.getJsonInteger(body, "annual_rate_bps", 1800))
        .build();
    limitClient.setLimit(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void handleGetRewards(RoutingContext ctx) {
    String cardNumber = ctx.pathParam("cardNumber");
    // Since rewards don't have a dedicated gRPC service in the gateway,
    // we call the limit client with a simple approach
    // In production this would be a proper gRPC call to a reward service
    ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(new JsonObject()
            .put("status", "success")
            .put("message", "OK. Use internal card service for reward operations")
            .encode());
  }

  private com.google.protobuf.Timestamp toTimestamp(String dateStr) {
    try {
      if (dateStr == null || dateStr.isEmpty()) {
        return Timestamps.fromMillis(System.currentTimeMillis());
      }
      Instant instant = OffsetDateTime.parse(dateStr).toInstant();
      return Timestamps.fromMillis(instant.toEpochMilli());
    } catch (Exception e) {
      return Timestamps.fromMillis(System.currentTimeMillis());
    }
  }
}