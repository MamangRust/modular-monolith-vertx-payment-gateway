package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.saldo.Saldo;
import pb.saldo.SaldoCommand;
import pb.saldo.VertxSaldoCommandServiceGrpcClient;
import pb.saldo.VertxSaldoQueryServiceGrpcClient;
import pb.saldo.stats.VertxSaldoStatsBalanceServiceGrpcClient;
import pb.saldo.stats.VertxSaldoStatsTotalBalanceGrpcClient;

public class SaldoProxyHandler {
  private final VertxSaldoQueryServiceGrpcClient queryClient;
  private final VertxSaldoCommandServiceGrpcClient commandClient;
  private final VertxSaldoStatsBalanceServiceGrpcClient balanceStatsClient;
  private final VertxSaldoStatsTotalBalanceGrpcClient totalStatsClient;

  public SaldoProxyHandler(
      VertxSaldoQueryServiceGrpcClient queryClient, 
      VertxSaldoCommandServiceGrpcClient commandClient, 
      VertxSaldoStatsBalanceServiceGrpcClient balanceStatsClient,
      VertxSaldoStatsTotalBalanceGrpcClient totalStatsClient) {
    this.queryClient = queryClient;
    this.commandClient = commandClient;
    this.balanceStatsClient = balanceStatsClient;
    this.totalStatsClient = totalStatsClient;
  }

  public void getAllSaldos(RoutingContext ctx) {
    var req = Saldo.FindAllSaldoRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();

    queryClient.findAllSaldo(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void getActiveSaldos(RoutingContext ctx) {
    var req = Saldo.FindAllSaldoRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();

    queryClient.findByActive(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void getTrashedSaldos(RoutingContext ctx) {
    var req = Saldo.FindAllSaldoRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();

    queryClient.findByTrashed(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void getSaldoById(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("saldoId"));
    var req = Saldo.FindByIdSaldoRequest.newBuilder().setSaldoId(id).build();

    queryClient.findByIdSaldo(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void createSaldo(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = SaldoCommand.CreateSaldoRequest.newBuilder()
        .setCardNumber(body.getString("card_number", ""))
        .setTotalBalance(body.getInteger("balance", 0))
        .build();

    commandClient.createSaldo(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 201))
        .onFailure(ctx::fail);
  }

  public void updateSaldo(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = SaldoCommand.UpdateSaldoRequest.newBuilder()
        .setSaldoId(body.getInteger("id", 0))
        .setCardNumber(body.getString("card_number", ""))
        .setTotalBalance(body.getInteger("balance", 0))
        .build();

    commandClient.updateSaldo(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void trashSaldo(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("saldoId"));
    var req = Saldo.FindByIdSaldoRequest.newBuilder().setSaldoId(id).build();

    commandClient.trashedSaldo(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void restoreSaldo(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("saldoId"));
    var req = Saldo.FindByIdSaldoRequest.newBuilder().setSaldoId(id).build();

    commandClient.restoreSaldo(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void deleteSaldoPermanently(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("saldoId"));
    var req = Saldo.FindByIdSaldoRequest.newBuilder().setSaldoId(id).build();

    commandClient.deleteSaldoPermanent(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void restoreAllSaldos(RoutingContext ctx) {
    commandClient.restoreAllSaldo(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void deleteAllPermanentSaldos(RoutingContext ctx) {
    commandClient.deleteAllSaldoPermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  // === STATS ===
  public void getMonthlyTotalSaldoBalance(RoutingContext ctx) {
    int year = ctx.queryParams().contains("year") ? Integer.parseInt(ctx.queryParams().get("year")) : 2024;
    int month = ctx.queryParams().contains("month") ? Integer.parseInt(ctx.queryParams().get("month")) : 1;
    var req = Saldo.FindMonthlySaldoTotalBalance.newBuilder()
        .setYear(year)
        .setMonth(month)
        .build();
    totalStatsClient.findMonthlyTotalSaldoBalance(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyTotalSaldoBalances(RoutingContext ctx) {
    int year = ctx.queryParams().contains("year") ? Integer.parseInt(ctx.queryParams().get("year")) : 2024;
    var req = Saldo.FindYearlySaldo.newBuilder().setYear(year).build();
    totalStatsClient.findYearTotalSaldoBalance(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void getMonthlySaldoBalances(RoutingContext ctx) {
    int year = ctx.queryParams().contains("year") ? Integer.parseInt(ctx.queryParams().get("year")) : 2024;
    var req = Saldo.FindYearlySaldo.newBuilder().setYear(year).build();
    balanceStatsClient.findMonthlySaldoBalances(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlySaldoBalances(RoutingContext ctx) {
    int year = ctx.queryParams().contains("year") ? Integer.parseInt(ctx.queryParams().get("year")) : 2024;
    var req = Saldo.FindYearlySaldo.newBuilder().setYear(year).build();
    balanceStatsClient.findYearlySaldoBalances(req)
        .onSuccess(resp -> sendResponse(ctx, resp, 200))
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
}
