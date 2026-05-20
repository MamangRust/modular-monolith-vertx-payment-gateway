package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.transfer.Transfer;
import pb.transfer.TransferCommand;
import pb.transfer.VertxTransferCommandServiceGrpcClient;
import pb.transfer.VertxTransferQueryServiceGrpcClient;
import pb.transfer.stats.VertxTransferStatsAmountServiceGrpcClient;
import pb.transfer.stats.VertxTransferStatsStatusServiceGrpcClient;

public class TransferProxyHandler {
  private final VertxTransferQueryServiceGrpcClient queryClient;
  private final VertxTransferCommandServiceGrpcClient commandClient;
  private final VertxTransferStatsAmountServiceGrpcClient statsAmountClient;
  private final VertxTransferStatsStatusServiceGrpcClient statsStatusClient;

  public TransferProxyHandler(
      VertxTransferQueryServiceGrpcClient queryClient, 
      VertxTransferCommandServiceGrpcClient commandClient, 
      VertxTransferStatsAmountServiceGrpcClient statsAmountClient,
      VertxTransferStatsStatusServiceGrpcClient statsStatusClient) {
    this.queryClient = queryClient;
    this.commandClient = commandClient;
    this.statsAmountClient = statsAmountClient;
    this.statsStatusClient = statsStatusClient;
  }

  private int getYearParam(RoutingContext ctx) {
    return ctx.queryParams().contains("year") ? Integer.parseInt(ctx.queryParams().get("year")) : 2024;
  }
  private int getMonthParam(RoutingContext ctx) {
    return ctx.queryParams().contains("month") ? Integer.parseInt(ctx.queryParams().get("month")) : 1;
  }

  // == BASIC ==
  public void getAllTransfers(RoutingContext ctx) {
    var req = Transfer.FindAllTransferRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findAllTransfer(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getActiveTransfers(RoutingContext ctx) {
    var req = Transfer.FindAllTransferRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByActiveTransfer(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTrashedTransfers(RoutingContext ctx) {
    var req = Transfer.FindAllTransferRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByTrashedTransfer(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTransferById(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("transferId"));
    var req = Transfer.FindByIdTransferRequest.newBuilder().setTransferId(id).build();
    queryClient.findByIdTransfer(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTransfersByCardNumber(RoutingContext ctx) {
    String card = ctx.pathParam("cardNumber");
    var req = Transfer.FindTransferByTransferFromRequest.newBuilder().setTransferFrom(card).build();
    queryClient.findTransferByTransferFrom(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTransfersAsSender(RoutingContext ctx) {
    String card = ctx.pathParam("cardNumber");
    var req = Transfer.FindTransferByTransferFromRequest.newBuilder().setTransferFrom(card).build();
    queryClient.findTransferByTransferFrom(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTransfersAsReceiver(RoutingContext ctx) {
    String card = ctx.pathParam("cardNumber");
    var req = Transfer.FindTransferByTransferToRequest.newBuilder().setTransferTo(card).build();
    queryClient.findTransferByTransferTo(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == COMMANDS ==
  public void createTransfer(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = TransferCommand.CreateTransferRequest.newBuilder()
        .setTransferFrom(body.getString("sender_card_number", ""))
        .setTransferTo(body.getString("receiver_card_number", ""))
        .setTransferAmount(body.getInteger("amount", 0))
        .build();
    commandClient.createTransfer(req).onSuccess(r -> sendResponse(ctx, r, 201)).onFailure(ctx::fail);
  }

  public void updateTransfer(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = TransferCommand.UpdateTransferRequest.newBuilder()
        .setTransferId(body.getInteger("id", 0))
        .setTransferFrom(body.getString("sender_card_number", ""))
        .setTransferTo(body.getString("receiver_card_number", ""))
        .setTransferAmount(body.getInteger("amount", 0))
        .build();
    commandClient.updateTransfer(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void trashTransfer(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("transferId"));
    var req = Transfer.FindByIdTransferRequest.newBuilder().setTransferId(id).build();
    commandClient.trashedTransfer(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreTransfer(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("transferId"));
    var req = Transfer.FindByIdTransferRequest.newBuilder().setTransferId(id).build();
    commandClient.restoreTransfer(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteTransferPermanent(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("transferId"));
    var req = Transfer.FindByIdTransferRequest.newBuilder().setTransferId(id).build();
    commandClient.deleteTransferPermanent(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreAllTransfers(RoutingContext ctx) {
    commandClient.restoreAllTransfer(com.google.protobuf.Empty.getDefaultInstance()).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteAllPermanentTransfers(RoutingContext ctx) {
    commandClient.deleteAllTransferPermanent(com.google.protobuf.Empty.getDefaultInstance()).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS GLOBAL ==
  public void getMonthTransferStatusSuccess(RoutingContext ctx) {
    var req = Transfer.FindMonthlyTransferStatus.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).build();
    statsStatusClient.findMonthlyTransferStatusSuccess(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransferStatusSuccess(RoutingContext ctx) {
    var req = Transfer.FindYearTransferStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsStatusClient.findYearlyTransferStatusSuccess(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthTransferStatusFailed(RoutingContext ctx) {
    var req = Transfer.FindMonthlyTransferStatus.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).build();
    statsStatusClient.findMonthlyTransferStatusFailed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransferStatusFailed(RoutingContext ctx) {
    var req = Transfer.FindYearTransferStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsStatusClient.findYearlyTransferStatusFailed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyTransferAmounts(RoutingContext ctx) {
    var req = Transfer.FindYearTransferStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsAmountClient.findMonthlyTransferAmounts(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransferAmounts(RoutingContext ctx) {
    var req = Transfer.FindYearTransferStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsAmountClient.findYearlyTransferAmounts(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS CARD ==
  public void getMonthTransferStatusSuccessCardNumber(RoutingContext ctx) {
    var req = Transfer.FindMonthlyTransferStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findMonthlyTransferStatusSuccessByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransferStatusSuccessCardNumber(RoutingContext ctx) {
    var req = Transfer.FindYearTransferStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findYearlyTransferStatusSuccessByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthTransferStatusFailedCardNumber(RoutingContext ctx) {
    var req = Transfer.FindMonthlyTransferStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findMonthlyTransferStatusFailedByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransferStatusFailedCardNumber(RoutingContext ctx) {
    var req = Transfer.FindYearTransferStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findYearlyTransferStatusFailedByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyTransferAmountsBySenderCardNumber(RoutingContext ctx) {
    var req = Transfer.FindByCardNumberTransferRequest.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsAmountClient.findMonthlyTransferAmountsBySenderCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransferAmountsBySenderCardNumber(RoutingContext ctx) {
    var req = Transfer.FindByCardNumberTransferRequest.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsAmountClient.findYearlyTransferAmountsBySenderCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyTransferAmountsByReceiverCardNumber(RoutingContext ctx) {
    var req = Transfer.FindByCardNumberTransferRequest.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsAmountClient.findMonthlyTransferAmountsByReceiverCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransferAmountsByReceiverCardNumber(RoutingContext ctx) {
    var req = Transfer.FindByCardNumberTransferRequest.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsAmountClient.findYearlyTransferAmountsByReceiverCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  private void sendResponse(RoutingContext ctx, com.google.protobuf.MessageOrBuilder proto, int defaultStatus) {
    JsonObject json = ProtoMapper.toJson(proto);
    int status = json.getInteger("status", defaultStatus);
    ctx.response()
        .setStatusCode(status == 0 ? defaultStatus : status)
        .putHeader("Content-Type", "application/json")
        .end(json.encode());
  }
}
