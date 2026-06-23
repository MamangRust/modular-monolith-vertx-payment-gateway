package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import pb.transfer.Transfer;
import pb.transfer.TransferCommand;
import pb.transfer.VertxTransferCommandServiceGrpcClient;
import pb.transfer.VertxTransferQueryServiceGrpcClient;
import pb.transfer.stats.VertxTransferStatsAmountServiceGrpcClient;
import pb.transfer.stats.VertxTransferStatsStatusServiceGrpcClient;

@RequiredArgsConstructor
public class TransferProxyHandler {
  private final VertxTransferQueryServiceGrpcClient queryClient;
  private final VertxTransferCommandServiceGrpcClient commandClient;
  private final VertxTransferStatsAmountServiceGrpcClient statsAmountClient;
  private final VertxTransferStatsStatusServiceGrpcClient statsStatusClient;

  private Transfer.FindAllTransferRequest buildFindAllTransferReq(RoutingContext ctx) {
    return Transfer.FindAllTransferRequest.newBuilder()
        .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
        .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
        .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
        .build();
  }

  private Transfer.FindYearTransferStatus buildFindYearTransferStatusReq(RoutingContext ctx) {
    return Transfer.FindYearTransferStatus.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .build();
  }

  private Transfer.FindByCardNumberTransferRequest buildFindByCardNumberTransferReq(RoutingContext ctx) {
    return Transfer.FindByCardNumberTransferRequest.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  private Transfer.FindMonthlyTransferStatus buildFindMonthlyTransferStatusReq(RoutingContext ctx) {
    return Transfer.FindMonthlyTransferStatus.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
        .build();
  }

  private Transfer.FindMonthlyTransferStatusCardNumber buildFindMonthlyTransferStatusCardNumberReq(RoutingContext ctx) {
    return Transfer.FindMonthlyTransferStatusCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  private Transfer.FindYearTransferStatusCardNumber buildFindYearTransferStatusCardNumberReq(RoutingContext ctx) {
    return Transfer.FindYearTransferStatusCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  public void getAllTransfers(RoutingContext ctx) {
    queryClient.findAllTransfer(buildFindAllTransferReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getActiveTransfers(RoutingContext ctx) {
    queryClient.findByActiveTransfer(buildFindAllTransferReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTrashedTransfers(RoutingContext ctx) {
    queryClient.findByTrashedTransfer(buildFindAllTransferReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTransferById(RoutingContext ctx) {
    var req = Transfer.FindByIdTransferRequest.newBuilder()
        .setTransferId(GrpcGatewayUtils.getSafePathInt(ctx, "transferId"))
        .build();
    queryClient.findByIdTransfer(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTransfersByCardNumber(RoutingContext ctx) {
    var req = Transfer.FindTransferByTransferFromRequest.newBuilder()
        .setTransferFrom(ctx.pathParam("cardNumber")).build();
    queryClient.findTransferByTransferFrom(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTransfersAsSender(RoutingContext ctx) {
    var req = Transfer.FindTransferByTransferFromRequest.newBuilder()
        .setTransferFrom(ctx.pathParam("cardNumber")).build();
    queryClient.findTransferByTransferFrom(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTransfersAsReceiver(RoutingContext ctx) {
    var req = Transfer.FindTransferByTransferToRequest.newBuilder()
        .setTransferTo(ctx.pathParam("cardNumber")).build();
    queryClient.findTransferByTransferTo(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void createTransfer(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = TransferCommand.CreateTransferRequest.newBuilder()
        .setTransferFrom(GrpcGatewayUtils.getJsonString(body, "sender_card_number", ""))
        .setTransferTo(GrpcGatewayUtils.getJsonString(body, "receiver_card_number", ""))
        .setTransferAmount(GrpcGatewayUtils.getJsonInteger(body, "amount", 0))
        .build();
    commandClient.createTransfer(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 201))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void updateTransfer(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = TransferCommand.UpdateTransferRequest.newBuilder()
        .setTransferId(GrpcGatewayUtils.getJsonInteger(body, "id", 0))
        .setTransferFrom(GrpcGatewayUtils.getJsonString(body, "sender_card_number", ""))
        .setTransferTo(GrpcGatewayUtils.getJsonString(body, "receiver_card_number", ""))
        .setTransferAmount(GrpcGatewayUtils.getJsonInteger(body, "amount", 0))
        .build();
    commandClient.updateTransfer(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void trashTransfer(RoutingContext ctx) {
    var req = Transfer.FindByIdTransferRequest.newBuilder()
        .setTransferId(GrpcGatewayUtils.getSafePathInt(ctx, "transferId"))
        .build();
    commandClient.trashedTransfer(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreTransfer(RoutingContext ctx) {
    var req = Transfer.FindByIdTransferRequest.newBuilder()
        .setTransferId(GrpcGatewayUtils.getSafePathInt(ctx, "transferId"))
        .build();
    commandClient.restoreTransfer(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteTransferPermanent(RoutingContext ctx) {
    var req = Transfer.FindByIdTransferRequest.newBuilder()
        .setTransferId(GrpcGatewayUtils.getSafePathInt(ctx, "transferId"))
        .build();
    commandClient.deleteTransferPermanent(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreAllTransfers(RoutingContext ctx) {
    commandClient.restoreAllTransfer(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteAllPermanentTransfers(RoutingContext ctx) {
    commandClient.deleteAllTransferPermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTransferStatusSuccess(RoutingContext ctx) {
    statsStatusClient.findMonthlyTransferStatusSuccess(buildFindMonthlyTransferStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransferStatusSuccess(RoutingContext ctx) {
    statsStatusClient.findYearlyTransferStatusSuccess(buildFindYearTransferStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTransferStatusFailed(RoutingContext ctx) {
    statsStatusClient.findMonthlyTransferStatusFailed(buildFindMonthlyTransferStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransferStatusFailed(RoutingContext ctx) {
    statsStatusClient.findYearlyTransferStatusFailed(buildFindYearTransferStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTransferAmounts(RoutingContext ctx) {
    statsAmountClient.findMonthlyTransferAmounts(buildFindYearTransferStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransferAmounts(RoutingContext ctx) {
    statsAmountClient.findYearlyTransferAmounts(buildFindYearTransferStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTransferStatusSuccessCardNumber(RoutingContext ctx) {
    statsStatusClient.findMonthlyTransferStatusSuccessByCardNumber(buildFindMonthlyTransferStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransferStatusSuccessCardNumber(RoutingContext ctx) {
    statsStatusClient.findYearlyTransferStatusSuccessByCardNumber(buildFindYearTransferStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTransferStatusFailedCardNumber(RoutingContext ctx) {
    statsStatusClient.findMonthlyTransferStatusFailedByCardNumber(buildFindMonthlyTransferStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransferStatusFailedCardNumber(RoutingContext ctx) {
    statsStatusClient.findYearlyTransferStatusFailedByCardNumber(buildFindYearTransferStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTransferAmountsBySenderCardNumber(RoutingContext ctx) {
    statsAmountClient.findMonthlyTransferAmountsBySenderCardNumber(buildFindByCardNumberTransferReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransferAmountsBySenderCardNumber(RoutingContext ctx) {
    statsAmountClient.findYearlyTransferAmountsBySenderCardNumber(buildFindByCardNumberTransferReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTransferAmountsByReceiverCardNumber(RoutingContext ctx) {
    statsAmountClient.findMonthlyTransferAmountsByReceiverCardNumber(buildFindByCardNumberTransferReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransferAmountsByReceiverCardNumber(RoutingContext ctx) {
    statsAmountClient.findYearlyTransferAmountsByReceiverCardNumber(buildFindByCardNumberTransferReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }
}