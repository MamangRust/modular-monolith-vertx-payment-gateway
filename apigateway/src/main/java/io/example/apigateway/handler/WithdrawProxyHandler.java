package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.withdraw.Withdraw;
import pb.withdraw.WithdrawCommand;
import pb.withdraw.VertxWithdrawCommandServiceGrpcClient;
import pb.withdraw.VertxWithdrawQueryServiceGrpcClient;
import pb.withdraw.stats.VertxWithdrawStatsAmountServiceGrpcClient;
import pb.withdraw.stats.VertxWithdrawStatsStatusServiceGrpcClient;

public class WithdrawProxyHandler {
  private final VertxWithdrawQueryServiceGrpcClient queryClient;
  private final VertxWithdrawCommandServiceGrpcClient commandClient;
  private final VertxWithdrawStatsAmountServiceGrpcClient statsAmountClient;
  private final VertxWithdrawStatsStatusServiceGrpcClient statsStatusClient;

  public WithdrawProxyHandler(VertxWithdrawQueryServiceGrpcClient queryClient,
                               VertxWithdrawCommandServiceGrpcClient commandClient,
                               VertxWithdrawStatsAmountServiceGrpcClient statsAmountClient,
                               VertxWithdrawStatsStatusServiceGrpcClient statsStatusClient) {
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
  public void getAllWithdraws(RoutingContext ctx) {
    var req = Withdraw.FindAllWithdrawRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findAllWithdraw(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getActiveWithdraws(RoutingContext ctx) {
    var req = Withdraw.FindAllWithdrawRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByActive(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTrashedWithdraws(RoutingContext ctx) {
    var req = Withdraw.FindAllWithdrawRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByTrashed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getWithdrawById(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("withdrawId"));
    var req = Withdraw.FindByIdWithdrawRequest.newBuilder().setWithdrawId(id).build();
    queryClient.findByIdWithdraw(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == COMMANDS ==
  public void createWithdraw(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = WithdrawCommand.CreateWithdrawRequest.newBuilder()
        .setCardNumber(body.getString("card_number", ""))
        .setWithdrawAmount(body.getInteger("amount", 0))
        .build();
    commandClient.createWithdraw(req).onSuccess(r -> sendResponse(ctx, r, 201)).onFailure(ctx::fail);
  }

  public void updateWithdraw(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = WithdrawCommand.UpdateWithdrawRequest.newBuilder()
        .setWithdrawId(body.getInteger("id", 0))
        .setCardNumber(body.getString("card_number", ""))
        .setWithdrawAmount(body.getInteger("amount", 0))
        .build();
    commandClient.updateWithdraw(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void trash(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("withdrawId"));
    var req = Withdraw.FindByIdWithdrawRequest.newBuilder().setWithdrawId(id).build();
    commandClient.trashedWithdraw(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restore(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("withdrawId"));
    var req = Withdraw.FindByIdWithdrawRequest.newBuilder().setWithdrawId(id).build();
    commandClient.restoreWithdraw(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deletePermanent(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("withdrawId"));
    var req = Withdraw.FindByIdWithdrawRequest.newBuilder().setWithdrawId(id).build();
    commandClient.deleteWithdrawPermanent(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreAllWithdraws(RoutingContext ctx) {
    commandClient.restoreAllWithdraw(com.google.protobuf.Empty.getDefaultInstance()).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteAllPermanentWithdraws(RoutingContext ctx) {
    commandClient.deleteAllWithdrawPermanent(com.google.protobuf.Empty.getDefaultInstance()).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS STATUS ==
  public void getMonthWithdrawStatusSuccess(RoutingContext ctx) {
    var req = Withdraw.FindMonthlyWithdrawStatus.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).build();
    statsStatusClient.findMonthlyWithdrawStatusSuccess(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyWithdrawStatusSuccess(RoutingContext ctx) {
    var req = Withdraw.FindYearWithdrawStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsStatusClient.findYearlyWithdrawStatusSuccess(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthWithdrawStatusFailed(RoutingContext ctx) {
    var req = Withdraw.FindMonthlyWithdrawStatus.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).build();
    statsStatusClient.findMonthlyWithdrawStatusFailed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyWithdrawStatusFailed(RoutingContext ctx) {
    var req = Withdraw.FindYearWithdrawStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsStatusClient.findYearlyWithdrawStatusFailed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthWithdrawStatusSuccessCardNumber(RoutingContext ctx) {
    var req = Withdraw.FindMonthlyWithdrawStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findMonthlyWithdrawStatusSuccessCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyWithdrawStatusSuccessCardNumber(RoutingContext ctx) {
    var req = Withdraw.FindYearWithdrawStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findYearlyWithdrawStatusSuccessCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthWithdrawStatusFailedCardNumber(RoutingContext ctx) {
    var req = Withdraw.FindMonthlyWithdrawStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findMonthlyWithdrawStatusFailedCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyWithdrawStatusFailedCardNumber(RoutingContext ctx) {
    var req = Withdraw.FindYearWithdrawStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findYearlyWithdrawStatusFailedCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS AMOUNT ==
  public void getMonthlyWithdraws(RoutingContext ctx) {
    var req = Withdraw.FindYearWithdrawStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsAmountClient.findMonthlyWithdraws(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyWithdraws(RoutingContext ctx) {
    var req = Withdraw.FindYearWithdrawStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsAmountClient.findYearlyWithdraws(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyWithdrawsByCardNumber(RoutingContext ctx) {
    var req = Withdraw.FindYearWithdrawCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsAmountClient.findMonthlyWithdrawsByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyWithdrawsByCardNumber(RoutingContext ctx) {
    var req = Withdraw.FindYearWithdrawCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsAmountClient.findYearlyWithdrawsByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
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
