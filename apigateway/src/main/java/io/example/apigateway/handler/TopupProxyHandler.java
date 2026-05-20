package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.topup.Topup;
import pb.topup.TopupCommand;
import pb.topup.TopupQuery;
import pb.topup.VertxTopupCommandServiceGrpcClient;
import pb.topup.VertxTopupQueryServiceGrpcClient;
import pb.topup.stats.VertxTopupStatsAmountServiceGrpcClient;
import pb.topup.stats.VertxTopupStatsMethodServiceGrpcClient;
import pb.topup.stats.VertxTopupStatsStatusServiceGrpcClient;

public class TopupProxyHandler {
  private final VertxTopupQueryServiceGrpcClient queryClient;
  private final VertxTopupCommandServiceGrpcClient commandClient;
  private final VertxTopupStatsAmountServiceGrpcClient statsAmountClient;
  private final VertxTopupStatsMethodServiceGrpcClient statsMethodClient;
  private final VertxTopupStatsStatusServiceGrpcClient statsStatusClient;

  public TopupProxyHandler(
      VertxTopupQueryServiceGrpcClient queryClient,
      VertxTopupCommandServiceGrpcClient commandClient,
      VertxTopupStatsAmountServiceGrpcClient statsAmountClient,
      VertxTopupStatsMethodServiceGrpcClient statsMethodClient,
      VertxTopupStatsStatusServiceGrpcClient statsStatusClient) {
    this.queryClient = queryClient;
    this.commandClient = commandClient;
    this.statsAmountClient = statsAmountClient;
    this.statsMethodClient = statsMethodClient;
    this.statsStatusClient = statsStatusClient;
  }

  private int getYearParam(RoutingContext ctx) {
    return ctx.queryParams().contains("year") ? Integer.parseInt(ctx.queryParams().get("year")) : 2024;
  }

  private int getMonthParam(RoutingContext ctx) {
    return ctx.queryParams().contains("month") ? Integer.parseInt(ctx.queryParams().get("month")) : 1;
  }

  // == BASIC ==
  public void getTopups(RoutingContext ctx) {
    var req = TopupQuery.FindAllTopupRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findAllTopup(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getActiveTopups(RoutingContext ctx) {
    var req = TopupQuery.FindAllTopupRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByActive(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTrashedTopups(RoutingContext ctx) {
    var req = TopupQuery.FindAllTopupRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByTrashed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTopupById(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("topupId"));
    var req = Topup.FindByIdTopupRequest.newBuilder().setTopupId(id).build();
    queryClient.findByIdTopup(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == COMMANDS ==
  public void createTopup(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = TopupCommand.CreateTopupRequest.newBuilder()
        .setCardNumber(body.getString("card_number", ""))
        .setTopupAmount(body.getInteger("amount", 0))
        .setTopupMethod(body.getString("topup_method", ""))
        .build();
    commandClient.createTopup(req).onSuccess(r -> sendResponse(ctx, r, 201)).onFailure(ctx::fail);
  }

  public void updateTopup(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = TopupCommand.UpdateTopupRequest.newBuilder()
        .setTopupId(body.getInteger("id", 0))
        .setCardNumber(body.getString("card_number", ""))
        .setTopupAmount(body.getInteger("amount", 0))
        .setTopupMethod(body.getString("topup_method", ""))
        .build();
    commandClient.updateTopup(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void trashTopup(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("topupId"));
    var req = Topup.FindByIdTopupRequest.newBuilder().setTopupId(id).build();
    commandClient.trashedTopup(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreTopup(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("topupId"));
    var req = Topup.FindByIdTopupRequest.newBuilder().setTopupId(id).build();
    commandClient.restoreTopup(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteTopupPermanently(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("topupId"));
    var req = Topup.FindByIdTopupRequest.newBuilder().setTopupId(id).build();
    commandClient.deleteTopupPermanent(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreAllTopups(RoutingContext ctx) {
    commandClient.restoreAllTopup(com.google.protobuf.Empty.getDefaultInstance()).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteAllPermanentTopups(RoutingContext ctx) {
    commandClient.deleteAllTopupPermanent(com.google.protobuf.Empty.getDefaultInstance()).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS STATUS GLOBAL ==
  public void getMonthTopupStatusSuccess(RoutingContext ctx) {
    var req = Topup.FindMonthlyTopupStatus.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).build();
    statsStatusClient.findMonthlyTopupStatusSuccess(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTopupStatusSuccess(RoutingContext ctx) {
    var req = Topup.FindYearTopupStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsStatusClient.findYearlyTopupStatusSuccess(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthTopupStatusFailed(RoutingContext ctx) {
    var req = Topup.FindMonthlyTopupStatus.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).build();
    statsStatusClient.findMonthlyTopupStatusFailed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTopupStatusFailed(RoutingContext ctx) {
    var req = Topup.FindYearTopupStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsStatusClient.findYearlyTopupStatusFailed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS STATUS CARD ==
  public void getMonthTopupStatusSuccessCardNumber(RoutingContext ctx) {
    var req = Topup.FindMonthlyTopupStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findMonthlyTopupStatusSuccessByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTopupStatusSuccessCardNumber(RoutingContext ctx) {
    var req = Topup.FindYearTopupStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findYearlyTopupStatusSuccessByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthTopupStatusFailedCardNumber(RoutingContext ctx) {
    var req = Topup.FindMonthlyTopupStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findMonthlyTopupStatusFailedByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTopupStatusFailedCardNumber(RoutingContext ctx) {
    var req = Topup.FindYearTopupStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findYearlyTopupStatusFailedByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS AMOUNT GLOBAL ==
  public void getMonthlyTopupAmounts(RoutingContext ctx) {
    var req = Topup.FindYearTopupStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsAmountClient.findMonthlyTopupAmounts(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTopupAmounts(RoutingContext ctx) {
    var req = Topup.FindYearTopupStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsAmountClient.findYearlyTopupAmounts(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS AMOUNT CARD ==
  public void getMonthlyTopupAmountsByCardNumber(RoutingContext ctx) {
    var req = Topup.FindYearTopupCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsAmountClient.findMonthlyTopupAmountsByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTopupAmountsByCardNumber(RoutingContext ctx) {
    var req = Topup.FindYearTopupCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsAmountClient.findYearlyTopupAmountsByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS METHOD GLOBAL ==
  public void getMonthlyTopupMethods(RoutingContext ctx) {
    var req = Topup.FindYearTopupStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsMethodClient.findMonthlyTopupMethods(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTopupMethods(RoutingContext ctx) {
    var req = Topup.FindYearTopupStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsMethodClient.findYearlyTopupMethods(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS METHOD CARD ==
  public void getMonthlyTopupMethodsByCardNumber(RoutingContext ctx) {
    var req = Topup.FindYearTopupCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsMethodClient.findMonthlyTopupMethodsByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTopupMethodsByCardNumber(RoutingContext ctx) {
    var req = Topup.FindYearTopupCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsMethodClient.findYearlyTopupMethodsByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
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
