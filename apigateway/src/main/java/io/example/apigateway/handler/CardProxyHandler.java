package io.example.apigateway.handler;

import java.time.Instant;
import java.time.OffsetDateTime;

import com.google.protobuf.util.Timestamps;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.card.Card;
import pb.card.CardCommand;
import pb.card.VertxCardCommandServiceGrpcClient;
import pb.card.VertxCardDashboardServiceGrpcClient;
import pb.card.VertxCardQueryServiceGrpcClient;
import pb.card.stats.VertxCardStatsBalanceServiceGrpcClient;
import pb.card.stats.VertxCardStatsTopupServiceGrpcClient;
import pb.card.stats.VertxCardStatsTransactionServiceGrpcClient;
import pb.card.stats.VertxCardStatsTransferServiceGrpcClient;
import pb.card.stats.VertxCardStatsWithdrawServiceGrpcClient;

public class CardProxyHandler {
  private final VertxCardQueryServiceGrpcClient queryClient;
  private final VertxCardCommandServiceGrpcClient commandClient;
  private final VertxCardDashboardServiceGrpcClient dashboardClient;
  private final VertxCardStatsBalanceServiceGrpcClient balanceClient;
  private final VertxCardStatsTopupServiceGrpcClient topupClient;
  private final VertxCardStatsWithdrawServiceGrpcClient withdrawClient;
  private final VertxCardStatsTransactionServiceGrpcClient transactionClient;
  private final VertxCardStatsTransferServiceGrpcClient transferClient;

  public CardProxyHandler(
      VertxCardQueryServiceGrpcClient queryClient,
      VertxCardCommandServiceGrpcClient commandClient,
      VertxCardDashboardServiceGrpcClient dashboardClient,
      VertxCardStatsBalanceServiceGrpcClient balanceClient,
      VertxCardStatsTopupServiceGrpcClient topupClient,
      VertxCardStatsWithdrawServiceGrpcClient withdrawClient,
      VertxCardStatsTransactionServiceGrpcClient transactionClient,
      VertxCardStatsTransferServiceGrpcClient transferClient) {
    this.queryClient = queryClient;
    this.commandClient = commandClient;
    this.dashboardClient = dashboardClient;
    this.balanceClient = balanceClient;
    this.topupClient = topupClient;
    this.withdrawClient = withdrawClient;
    this.transactionClient = transactionClient;
    this.transferClient = transferClient;
  }

  private int getYearParam(RoutingContext ctx) {
    return ctx.queryParams().contains("year") ? Integer.parseInt(ctx.queryParams().get("year")) : 2024;
  }

  private String getCardParam(RoutingContext ctx) {
    return ctx.queryParams().contains("card_number") ? ctx.queryParams().get("card_number") : "";
  }

  // == BASE QUERIES ==
  public void getAllCards(RoutingContext ctx) {
    var req = Card.FindAllCardRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findAllCard(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getActiveCards(RoutingContext ctx) {
    var req = Card.FindAllCardRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByActiveCard(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTrashedCards(RoutingContext ctx) {
    var req = Card.FindAllCardRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByTrashedCard(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getCardById(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("cardId"));
    var req = Card.FindByIdCardRequest.newBuilder().setCardId(id).build();
    queryClient.findByIdCard(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == BASE COMMANDS ==
  public void createCard(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = CardCommand.CreateCardRequest.newBuilder()
        .setUserId(body.getInteger("user_id", 0))
        .setCardType(body.getString("card_type", ""))
        .setExpireDate(toTimestamp(body.getString("expiry_date", "")))
        .setCvv(body.getString("cvv", ""))
        .setCardProvider(body.getString("card_provider", ""))
        .build();
    commandClient.createCard(req).onSuccess(r -> sendResponse(ctx, r, 201)).onFailure(ctx::fail);
  }

  public void updateCard(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("cardId"));
    JsonObject body = ctx.body().asJsonObject();
    var req = CardCommand.UpdateCardRequest.newBuilder()
        .setCardId(id)
        .setUserId(body.getInteger("user_id", 0))
        .setCardType(body.getString("card_type", ""))
        .setExpireDate(toTimestamp(body.getString("expiry_date", "")))
        .setCvv(body.getString("cvv", ""))
        .setCardProvider(body.getString("card_provider", ""))
        .build();
    commandClient.updateCard(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void trashCard(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("cardId"));
    var req = Card.FindByIdCardRequest.newBuilder().setCardId(id).build();
    commandClient.trashedCard(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreCard(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("cardId"));
    var req = Card.FindByIdCardRequest.newBuilder().setCardId(id).build();
    commandClient.restoreCard(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deletePermanent(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("cardId"));
    var req = Card.FindByIdCardRequest.newBuilder().setCardId(id).build();
    commandClient.deleteCardPermanent(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreAllCards(RoutingContext ctx) {
    commandClient.restoreAllCard(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteAllPermanentCards(RoutingContext ctx) {
    commandClient.deleteAllCardPermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // === STATS / DASHBOARD GLOBAL ===
  public void getDashboard(RoutingContext ctx) {
    dashboardClient.dashboardCard(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getDashboardByCardNumber(RoutingContext ctx) {
    String num = ctx.pathParam("cardNumber");
    var req = Card.FindByCardNumberRequest.newBuilder().setCardNumber(num).build();
    dashboardClient.dashboardCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyBalances(RoutingContext ctx) {
    var req = pb.card.stats.CardStatsBalance.FindYearBalance.newBuilder().setYear(getYearParam(ctx)).build();
    balanceClient.findMonthlyBalance(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyBalances(RoutingContext ctx) {
    var req = pb.card.stats.CardStatsBalance.FindYearBalance.newBuilder().setYear(getYearParam(ctx)).build();
    balanceClient.findYearlyBalance(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyTopupAmount(RoutingContext ctx) {
    var req = Card.FindYearAmount.newBuilder().setYear(getYearParam(ctx)).build();
    topupClient.findMonthlyTopupAmount(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTopupAmount(RoutingContext ctx) {
    var req = Card.FindYearAmount.newBuilder().setYear(getYearParam(ctx)).build();
    topupClient.findYearlyTopupAmount(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyWithdrawAmount(RoutingContext ctx) {
    var req = Card.FindYearAmount.newBuilder().setYear(getYearParam(ctx)).build();
    withdrawClient.findMonthlyWithdrawAmount(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyWithdrawAmount(RoutingContext ctx) {
    var req = Card.FindYearAmount.newBuilder().setYear(getYearParam(ctx)).build();
    withdrawClient.findYearlyWithdrawAmount(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyTransactionAmount(RoutingContext ctx) {
    var req = Card.FindYearAmount.newBuilder().setYear(getYearParam(ctx)).build();
    transactionClient.findMonthlyTransactionAmount(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransactionAmount(RoutingContext ctx) {
    var req = Card.FindYearAmount.newBuilder().setYear(getYearParam(ctx)).build();
    transactionClient.findYearlyTransactionAmount(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyTransferAmountSender(RoutingContext ctx) {
    var req = Card.FindYearAmount.newBuilder().setYear(getYearParam(ctx)).build();
    transferClient.findMonthlyTransferSenderAmount(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransferAmountSender(RoutingContext ctx) {
    var req = Card.FindYearAmount.newBuilder().setYear(getYearParam(ctx)).build();
    transferClient.findYearlyTransferSenderAmount(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyTransferAmountReceiver(RoutingContext ctx) {
    var req = Card.FindYearAmount.newBuilder().setYear(getYearParam(ctx)).build();
    transferClient.findMonthlyTransferReceiverAmount(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyTransferAmountReceiver(RoutingContext ctx) {
    var req = Card.FindYearAmount.newBuilder().setYear(getYearParam(ctx)).build();
    transferClient.findYearlyTransferReceiverAmount(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // === STATS / DASHBOARD BY CARD ===
  public void getMonthlyBalancesByCardNumber(RoutingContext ctx) {
    var req = pb.card.stats.CardStatsBalance.FindYearBalanceCardNumber.newBuilder().setYear(getYearParam(ctx))
        .setCardNumber(getCardParam(ctx)).build();
    balanceClient.findMonthlyBalanceByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyBalancesByCardNumber(RoutingContext ctx) {
    var req = pb.card.stats.CardStatsBalance.FindYearBalanceCardNumber.newBuilder().setYear(getYearParam(ctx))
        .setCardNumber(getCardParam(ctx)).build();
    balanceClient.findYearlyBalanceByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyTopupAmountByCardNumber(RoutingContext ctx) {
    var req = Card.FindYearAmountCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(getCardParam(ctx))
        .build();
    topupClient.findMonthlyTopupAmountByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTopupAmountByCardNumber(RoutingContext ctx) {
    var req = Card.FindYearAmountCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(getCardParam(ctx))
        .build();
    topupClient.findYearlyTopupAmountByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyWithdrawAmountByCardNumber(RoutingContext ctx) {
    var req = Card.FindYearAmountCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(getCardParam(ctx))
        .build();
    withdrawClient.findMonthlyWithdrawAmountByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyWithdrawAmountByCardNumber(RoutingContext ctx) {
    var req = Card.FindYearAmountCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(getCardParam(ctx))
        .build();
    withdrawClient.findYearlyWithdrawAmountByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getMonthlyTransactionAmountByCardNumber(RoutingContext ctx) {
    var req = Card.FindYearAmountCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(getCardParam(ctx))
        .build();
    transactionClient.findMonthlyTransactionAmountByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyTransactionAmountByCardNumber(RoutingContext ctx) {
    var req = Card.FindYearAmountCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(getCardParam(ctx))
        .build();
    transactionClient.findYearlyTransactionAmountByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getMonthlyTransferAmountBySender(RoutingContext ctx) {
    var req = Card.FindYearAmountCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(getCardParam(ctx))
        .build();
    transferClient.findMonthlyTransferSenderAmountByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyTransferAmountBySender(RoutingContext ctx) {
    var req = Card.FindYearAmountCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(getCardParam(ctx))
        .build();
    transferClient.findYearlyTransferSenderAmountByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getMonthlyTransferAmountByReceiver(RoutingContext ctx) {
    var req = Card.FindYearAmountCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(getCardParam(ctx))
        .build();
    transferClient.findMonthlyTransferReceiverAmountByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyTransferAmountByReceiver(RoutingContext ctx) {
    var req = Card.FindYearAmountCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(getCardParam(ctx))
        .build();
    transferClient.findYearlyTransferReceiverAmountByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  private void sendResponse(RoutingContext ctx, com.google.protobuf.MessageOrBuilder proto, int defaultStatus) {
    JsonObject json = ProtoMapper.toJson(proto);
    int status = json.getInteger("status", defaultStatus);
    ctx.response()
        .setStatusCode(status == 0 ? defaultStatus : status)
        .putHeader("Content-Type", "application/json")
        .end(json.encode());
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
