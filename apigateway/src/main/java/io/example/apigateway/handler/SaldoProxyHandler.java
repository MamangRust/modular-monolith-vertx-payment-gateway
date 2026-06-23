package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import pb.saldo.Saldo;
import pb.saldo.SaldoCommand;
import pb.saldo.VertxSaldoCommandServiceGrpcClient;
import pb.saldo.VertxSaldoQueryServiceGrpcClient;
import pb.saldo.stats.VertxSaldoStatsBalanceServiceGrpcClient;
import pb.saldo.stats.VertxSaldoStatsTotalBalanceGrpcClient;

@RequiredArgsConstructor
public class SaldoProxyHandler {
  private final VertxSaldoQueryServiceGrpcClient queryClient;
  private final VertxSaldoCommandServiceGrpcClient commandClient;
  private final VertxSaldoStatsBalanceServiceGrpcClient balanceStatsClient;
  private final VertxSaldoStatsTotalBalanceGrpcClient totalStatsClient;

  private Saldo.FindAllSaldoRequest buildFindAllSaldoReq(RoutingContext ctx) {
    return Saldo.FindAllSaldoRequest.newBuilder()
        .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
        .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
        .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
        .build();
  }

  private Saldo.FindYearlySaldo buildFindYearlySaldoReq(RoutingContext ctx) {
    return Saldo.FindYearlySaldo.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .build();
  }

  public void getAllSaldos(RoutingContext ctx) {
    queryClient.findAllSaldo(buildFindAllSaldoReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getActiveSaldos(RoutingContext ctx) {
    queryClient.findByActive(buildFindAllSaldoReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTrashedSaldos(RoutingContext ctx) {
    queryClient.findByTrashed(buildFindAllSaldoReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getSaldoById(RoutingContext ctx) {
    var req = Saldo.FindByIdSaldoRequest.newBuilder()
        .setSaldoId(GrpcGatewayUtils.getSafePathInt(ctx, "saldoId"))
        .build();
    queryClient.findByIdSaldo(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void createSaldo(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = SaldoCommand.CreateSaldoRequest.newBuilder()
        .setCardNumber(GrpcGatewayUtils.getJsonString(body, "card_number", ""))
        .setTotalBalance(GrpcGatewayUtils.getJsonInteger(body, "balance", 0))
        .build();
    commandClient.createSaldo(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 201))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void updateSaldo(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = SaldoCommand.UpdateSaldoRequest.newBuilder()
        .setSaldoId(GrpcGatewayUtils.getJsonInteger(body, "id", 0))
        .setCardNumber(GrpcGatewayUtils.getJsonString(body, "card_number", ""))
        .setTotalBalance(GrpcGatewayUtils.getJsonInteger(body, "balance", 0))
        .build();
    commandClient.updateSaldo(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void trashSaldo(RoutingContext ctx) {
    var req = Saldo.FindByIdSaldoRequest.newBuilder()
        .setSaldoId(GrpcGatewayUtils.getSafePathInt(ctx, "saldoId"))
        .build();
    commandClient.trashedSaldo(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreSaldo(RoutingContext ctx) {
    var req = Saldo.FindByIdSaldoRequest.newBuilder()
        .setSaldoId(GrpcGatewayUtils.getSafePathInt(ctx, "saldoId"))
        .build();
    commandClient.restoreSaldo(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteSaldoPermanently(RoutingContext ctx) {
    var req = Saldo.FindByIdSaldoRequest.newBuilder()
        .setSaldoId(GrpcGatewayUtils.getSafePathInt(ctx, "saldoId"))
        .build();
    commandClient.deleteSaldoPermanent(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreAllSaldos(RoutingContext ctx) {
    commandClient.restoreAllSaldo(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteAllPermanentSaldos(RoutingContext ctx) {
    commandClient.deleteAllSaldoPermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyTotalSaldoBalance(RoutingContext ctx) {
    var req = Saldo.FindMonthlySaldoTotalBalance.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
        .build();
    totalStatsClient.findMonthlyTotalSaldoBalance(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTotalSaldoBalances(RoutingContext ctx) {
    totalStatsClient.findYearTotalSaldoBalance(buildFindYearlySaldoReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlySaldoBalances(RoutingContext ctx) {
    balanceStatsClient.findMonthlySaldoBalances(buildFindYearlySaldoReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlySaldoBalances(RoutingContext ctx) {
    balanceStatsClient.findYearlySaldoBalances(buildFindYearlySaldoReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }
}