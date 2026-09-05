package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import pb.topup.Topup;
import pb.topup.TopupCommand;
import pb.topup.TopupQuery;
import pb.topup.VertxTopupCommandServiceGrpcClient;
import pb.topup.VertxTopupQueryServiceGrpcClient;
import pb.topup.stats.VertxTopupStatsAmountServiceGrpcClient;
import pb.topup.stats.VertxTopupStatsMethodServiceGrpcClient;
import pb.topup.stats.VertxTopupStatsStatusServiceGrpcClient;

@RequiredArgsConstructor
public class TopupProxyHandler {
  private final VertxTopupQueryServiceGrpcClient queryClient;
  private final VertxTopupCommandServiceGrpcClient commandClient;
  private final VertxTopupStatsAmountServiceGrpcClient statsAmountClient;
  private final VertxTopupStatsMethodServiceGrpcClient statsMethodClient;
  private final VertxTopupStatsStatusServiceGrpcClient statsStatusClient;

  private TopupQuery.FindAllTopupRequest buildFindAllTopupReq(RoutingContext ctx) {
    return TopupQuery.FindAllTopupRequest.newBuilder()
        .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
        .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
        .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
        .build();
  }

  private Topup.FindYearTopupStatus buildFindYearTopupStatusReq(RoutingContext ctx) {
    return Topup.FindYearTopupStatus.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .build();
  }

  private Topup.FindYearTopupCardNumber buildFindYearTopupCardNumberReq(RoutingContext ctx) {
    return Topup.FindYearTopupCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  private Topup.FindYearTopupStatusCardNumber buildFindYearTopupStatusCardNumberReq(RoutingContext ctx) {
    return Topup.FindYearTopupStatusCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  private Topup.FindMonthlyTopupStatus buildFindMonthlyTopupStatusReq(RoutingContext ctx) {
    return Topup.FindMonthlyTopupStatus.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
        .build();
  }

  private Topup.FindMonthlyTopupStatusCardNumber buildFindMonthlyTopupStatusCardNumberReq(RoutingContext ctx) {
    return Topup.FindMonthlyTopupStatusCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  public void getTopups(RoutingContext ctx) {
    queryClient.findAllTopup(buildFindAllTopupReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getActiveTopups(RoutingContext ctx) {
    queryClient.findByActive(buildFindAllTopupReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTrashedTopups(RoutingContext ctx) {
    queryClient.findByTrashed(buildFindAllTopupReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTopupById(RoutingContext ctx) {
    var req = Topup.FindByIdTopupRequest.newBuilder()
        .setTopupId(GrpcGatewayUtils.getSafePathInt(ctx, "topupId"))
        .build();
    queryClient.findByIdTopup(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void createTopup(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = TopupCommand.CreateTopupRequest.newBuilder()
        .setCardNumber(GrpcGatewayUtils.getJsonString(body, "card_number", ""))
        .setTopupAmount(GrpcGatewayUtils.getJsonInteger(body, "amount", 0))
        .setTopupMethod(GrpcGatewayUtils.getJsonString(body, "topup_method", ""))
        .setIdempotencyKey(GrpcGatewayUtils.getJsonString(body, "idempotency_key", ""))
        .build();
    commandClient.createTopup(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 201))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void updateTopup(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = TopupCommand.UpdateTopupRequest.newBuilder()
        .setTopupId(GrpcGatewayUtils.getJsonInteger(body, "id", 0))
        .setCardNumber(GrpcGatewayUtils.getJsonString(body, "card_number", ""))
        .setTopupAmount(GrpcGatewayUtils.getJsonInteger(body, "amount", 0))
        .setTopupMethod(GrpcGatewayUtils.getJsonString(body, "topup_method", ""))
        .build();
    commandClient.updateTopup(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void trashTopup(RoutingContext ctx) {
    var req = Topup.FindByIdTopupRequest.newBuilder()
        .setTopupId(GrpcGatewayUtils.getSafePathInt(ctx, "topupId"))
        .build();
    commandClient.trashedTopup(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreTopup(RoutingContext ctx) {
    var req = Topup.FindByIdTopupRequest.newBuilder()
        .setTopupId(GrpcGatewayUtils.getSafePathInt(ctx, "topupId"))
        .build();
    commandClient.restoreTopup(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteTopupPermanently(RoutingContext ctx) {
    var req = Topup.FindByIdTopupRequest.newBuilder()
        .setTopupId(GrpcGatewayUtils.getSafePathInt(ctx, "topupId"))
        .build();
    commandClient.deleteTopupPermanent(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreAllTopups(RoutingContext ctx) {
    commandClient.restoreAllTopup(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteAllPermanentTopups(RoutingContext ctx) {
    commandClient.deleteAllTopupPermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTopupStatusSuccess(RoutingContext ctx) {
    statsStatusClient.findMonthlyTopupStatusSuccess(buildFindMonthlyTopupStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTopupStatusSuccess(RoutingContext ctx) {
    statsStatusClient.findYearlyTopupStatusSuccess(buildFindYearTopupStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTopupStatusFailed(RoutingContext ctx) {
    statsStatusClient.findMonthlyTopupStatusFailed(buildFindMonthlyTopupStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTopupStatusFailed(RoutingContext ctx) {
    statsStatusClient.findYearlyTopupStatusFailed(buildFindYearTopupStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTopupStatusSuccessCardNumber(RoutingContext ctx) {
    statsStatusClient.findMonthlyTopupStatusSuccessByCardNumber(buildFindMonthlyTopupStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTopupStatusSuccessCardNumber(RoutingContext ctx) {
    statsStatusClient.findYearlyTopupStatusSuccessByCardNumber(buildFindYearTopupStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTopupStatusFailedCardNumber(RoutingContext ctx) {
    statsStatusClient.findMonthlyTopupStatusFailedByCardNumber(buildFindMonthlyTopupStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTopupStatusFailedCardNumber(RoutingContext ctx) {
    statsStatusClient.findYearlyTopupStatusFailedByCardNumber(buildFindYearTopupStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTopupAmounts(RoutingContext ctx) {
    statsAmountClient.findMonthlyTopupAmounts(buildFindYearTopupStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTopupAmounts(RoutingContext ctx) {
    statsAmountClient.findYearlyTopupAmounts(buildFindYearTopupStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTopupAmountsByCardNumber(RoutingContext ctx) {
    statsAmountClient.findMonthlyTopupAmountsByCardNumber(buildFindYearTopupCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTopupAmountsByCardNumber(RoutingContext ctx) {
    statsAmountClient.findYearlyTopupAmountsByCardNumber(buildFindYearTopupCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTopupMethods(RoutingContext ctx) {
    statsMethodClient.findMonthlyTopupMethods(buildFindYearTopupStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTopupMethods(RoutingContext ctx) {
    statsMethodClient.findYearlyTopupMethods(buildFindYearTopupStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTopupMethodsByCardNumber(RoutingContext ctx) {
    statsMethodClient.findMonthlyTopupMethodsByCardNumber(buildFindYearTopupCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTopupMethodsByCardNumber(RoutingContext ctx) {
    statsMethodClient.findYearlyTopupMethodsByCardNumber(buildFindYearTopupCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }
}