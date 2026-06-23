package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import pb.withdraw.Withdraw;
import pb.withdraw.WithdrawCommand;
import pb.withdraw.VertxWithdrawCommandServiceGrpcClient;
import pb.withdraw.VertxWithdrawQueryServiceGrpcClient;
import pb.withdraw.stats.VertxWithdrawStatsAmountServiceGrpcClient;
import pb.withdraw.stats.VertxWithdrawStatsStatusServiceGrpcClient;

@RequiredArgsConstructor
public class WithdrawProxyHandler {
  private final VertxWithdrawQueryServiceGrpcClient queryClient;
  private final VertxWithdrawCommandServiceGrpcClient commandClient;
  private final VertxWithdrawStatsAmountServiceGrpcClient statsAmountClient;
  private final VertxWithdrawStatsStatusServiceGrpcClient statsStatusClient;

  private Withdraw.FindAllWithdrawRequest buildFindAllWithdrawReq(RoutingContext ctx) {
    return Withdraw.FindAllWithdrawRequest.newBuilder()
        .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
        .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
        .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
        .build();
  }

  private Withdraw.FindYearWithdrawStatus buildFindYearWithdrawStatusReq(RoutingContext ctx) {
    return Withdraw.FindYearWithdrawStatus.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .build();
  }

  private Withdraw.FindYearWithdrawCardNumber buildFindYearWithdrawCardNumberReq(RoutingContext ctx) {
    return Withdraw.FindYearWithdrawCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  private Withdraw.FindMonthlyWithdrawStatus buildFindMonthlyWithdrawStatusReq(RoutingContext ctx) {
    return Withdraw.FindMonthlyWithdrawStatus.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
        .build();
  }

  private Withdraw.FindMonthlyWithdrawStatusCardNumber buildFindMonthlyWithdrawStatusCardNumberReq(RoutingContext ctx) {
    return Withdraw.FindMonthlyWithdrawStatusCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  private Withdraw.FindYearWithdrawStatusCardNumber buildFindYearWithdrawStatusCardNumberReq(RoutingContext ctx) {
    return Withdraw.FindYearWithdrawStatusCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  public void getAllWithdraws(RoutingContext ctx) {
    queryClient.findAllWithdraw(buildFindAllWithdrawReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getActiveWithdraws(RoutingContext ctx) {
    queryClient.findByActive(buildFindAllWithdrawReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTrashedWithdraws(RoutingContext ctx) {
    queryClient.findByTrashed(buildFindAllWithdrawReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getWithdrawById(RoutingContext ctx) {
    var req = Withdraw.FindByIdWithdrawRequest.newBuilder()
        .setWithdrawId(GrpcGatewayUtils.getSafePathInt(ctx, "withdrawId"))
        .build();
    queryClient.findByIdWithdraw(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void createWithdraw(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = WithdrawCommand.CreateWithdrawRequest.newBuilder()
        .setCardNumber(GrpcGatewayUtils.getJsonString(body, "card_number", ""))
        .setWithdrawAmount(GrpcGatewayUtils.getJsonInteger(body, "amount", 0))
        .build();
    commandClient.createWithdraw(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 201))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void updateWithdraw(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = WithdrawCommand.UpdateWithdrawRequest.newBuilder()
        .setWithdrawId(GrpcGatewayUtils.getJsonInteger(body, "id", 0))
        .setCardNumber(GrpcGatewayUtils.getJsonString(body, "card_number", ""))
        .setWithdrawAmount(GrpcGatewayUtils.getJsonInteger(body, "amount", 0))
        .build();
    commandClient.updateWithdraw(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void trash(RoutingContext ctx) {
    var req = Withdraw.FindByIdWithdrawRequest.newBuilder()
        .setWithdrawId(GrpcGatewayUtils.getSafePathInt(ctx, "withdrawId"))
        .build();
    commandClient.trashedWithdraw(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restore(RoutingContext ctx) {
    var req = Withdraw.FindByIdWithdrawRequest.newBuilder()
        .setWithdrawId(GrpcGatewayUtils.getSafePathInt(ctx, "withdrawId"))
        .build();
    commandClient.restoreWithdraw(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deletePermanent(RoutingContext ctx) {
    var req = Withdraw.FindByIdWithdrawRequest.newBuilder()
        .setWithdrawId(GrpcGatewayUtils.getSafePathInt(ctx, "withdrawId"))
        .build();
    commandClient.deleteWithdrawPermanent(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreAllWithdraws(RoutingContext ctx) {
    commandClient.restoreAllWithdraw(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteAllPermanentWithdraws(RoutingContext ctx) {
    commandClient.deleteAllWithdrawPermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthWithdrawStatusSuccess(RoutingContext ctx) {
    statsStatusClient.findMonthlyWithdrawStatusSuccess(buildFindMonthlyWithdrawStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyWithdrawStatusSuccess(RoutingContext ctx) {
    statsStatusClient.findYearlyWithdrawStatusSuccess(buildFindYearWithdrawStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthWithdrawStatusFailed(RoutingContext ctx) {
    statsStatusClient.findMonthlyWithdrawStatusFailed(buildFindMonthlyWithdrawStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyWithdrawStatusFailed(RoutingContext ctx) {
    statsStatusClient.findYearlyWithdrawStatusFailed(buildFindYearWithdrawStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthWithdrawStatusSuccessCardNumber(RoutingContext ctx) {
    statsStatusClient.findMonthlyWithdrawStatusSuccessCardNumber(buildFindMonthlyWithdrawStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyWithdrawStatusSuccessCardNumber(RoutingContext ctx) {
    statsStatusClient.findYearlyWithdrawStatusSuccessCardNumber(buildFindYearWithdrawStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthWithdrawStatusFailedCardNumber(RoutingContext ctx) {
    statsStatusClient.findMonthlyWithdrawStatusFailedCardNumber(buildFindMonthlyWithdrawStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyWithdrawStatusFailedCardNumber(RoutingContext ctx) {
    statsStatusClient.findYearlyWithdrawStatusFailedCardNumber(buildFindYearWithdrawStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyWithdraws(RoutingContext ctx) {
    statsAmountClient.findMonthlyWithdraws(buildFindYearWithdrawStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyWithdraws(RoutingContext ctx) {
    statsAmountClient.findYearlyWithdraws(buildFindYearWithdrawStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyWithdrawsByCardNumber(RoutingContext ctx) {
    statsAmountClient.findMonthlyWithdrawsByCardNumber(buildFindYearWithdrawCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyWithdrawsByCardNumber(RoutingContext ctx) {
    statsAmountClient.findYearlyWithdrawsByCardNumber(buildFindYearWithdrawCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }
}