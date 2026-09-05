package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.saldo.Saldo;
import pb.saldo.SaldoCommand;
import pb.saldo.VertxSaldoCommandServiceGrpcClient;
import pb.saldo.VertxSaldoQueryServiceGrpcClient;
import pb.saldo.stats.VertxSaldoStatsBalanceServiceGrpcClient;
import pb.saldo.stats.VertxSaldoStatsTotalBalanceGrpcClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaldoProxyHandlerTest {

  @Mock private VertxSaldoQueryServiceGrpcClient queryClient;
  @Mock private VertxSaldoCommandServiceGrpcClient commandClient;
  @Mock private VertxSaldoStatsBalanceServiceGrpcClient balanceStatsClient;
  @Mock private VertxSaldoStatsTotalBalanceGrpcClient totalStatsClient;
  @Mock private RoutingContext ctx;
  @Mock private RequestBody reqBody;

  private SaldoProxyHandler handler;
  private MockedStatic<GrpcGatewayUtils> utils;

  @BeforeEach
  void setUp() {
    handler = new SaldoProxyHandler(queryClient, commandClient, balanceStatsClient, totalStatsClient);
    utils = mockStatic(GrpcGatewayUtils.class);
  }

  @AfterEach
  void tearDown() {
    utils.close();
  }

  @Test
  void getAllSaldos() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);

    when(queryClient.findAllSaldo(any(Saldo.FindAllSaldoRequest.class)))
        .thenReturn(Future.succeededFuture(pb.saldo.SaldoQuery.ApiResponsePaginationSaldo.getDefaultInstance()));

    handler.getAllSaldos(ctx);
    verify(queryClient).findAllSaldo(any(Saldo.FindAllSaldoRequest.class));
  }

  @Test
  void getActiveSaldos() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);

    when(queryClient.findByActive(any(Saldo.FindAllSaldoRequest.class)))
        .thenReturn(Future.succeededFuture(pb.saldo.SaldoQuery.ApiResponsePaginationSaldoDeleteAt.getDefaultInstance()));

    handler.getActiveSaldos(ctx);
    verify(queryClient).findByActive(any(Saldo.FindAllSaldoRequest.class));
  }

  @Test
  void getTrashedSaldos() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);

    when(queryClient.findByTrashed(any(Saldo.FindAllSaldoRequest.class)))
        .thenReturn(Future.succeededFuture(pb.saldo.SaldoQuery.ApiResponsePaginationSaldoDeleteAt.getDefaultInstance()));

    handler.getTrashedSaldos(ctx);
    verify(queryClient).findByTrashed(any(Saldo.FindAllSaldoRequest.class));
  }

  @Test
  void getSaldoById() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "saldoId")).thenReturn(10);

    when(queryClient.findByIdSaldo(any(Saldo.FindByIdSaldoRequest.class)))
        .thenReturn(Future.succeededFuture(Saldo.ApiResponseSaldo.getDefaultInstance()));

    handler.getSaldoById(ctx);
    verify(queryClient).findByIdSaldo(any(Saldo.FindByIdSaldoRequest.class));
  }

  @Test
  void createSaldo() {
    var body = new JsonObject().put("card_number", "123").put("balance", 100);
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("card_number"), anyString())).thenReturn("123");
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("balance"), anyInt())).thenReturn(100);

    when(commandClient.createSaldo(any(SaldoCommand.CreateSaldoRequest.class)))
        .thenReturn(Future.succeededFuture(Saldo.ApiResponseSaldo.getDefaultInstance()));

    handler.createSaldo(ctx);
    verify(commandClient).createSaldo(any(SaldoCommand.CreateSaldoRequest.class));
  }

  @Test
  void updateSaldo() {
    var body = new JsonObject().put("id", 10).put("card_number", "123").put("balance", 200);
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("id"), anyInt())).thenReturn(10);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("card_number"), anyString())).thenReturn("123");
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("balance"), anyInt())).thenReturn(200);

    when(commandClient.updateSaldo(any(SaldoCommand.UpdateSaldoRequest.class)))
        .thenReturn(Future.succeededFuture(Saldo.ApiResponseSaldo.getDefaultInstance()));

    handler.updateSaldo(ctx);
    verify(commandClient).updateSaldo(any(SaldoCommand.UpdateSaldoRequest.class));
  }

  @Test
  void trashSaldo() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "saldoId")).thenReturn(10);

    when(commandClient.trashedSaldo(any(Saldo.FindByIdSaldoRequest.class)))
        .thenReturn(Future.succeededFuture(Saldo.ApiResponseSaldoDeleteAt.getDefaultInstance()));

    handler.trashSaldo(ctx);
    verify(commandClient).trashedSaldo(any(Saldo.FindByIdSaldoRequest.class));
  }

  @Test
  void restoreSaldo() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "saldoId")).thenReturn(10);

    when(commandClient.restoreSaldo(any(Saldo.FindByIdSaldoRequest.class)))
        .thenReturn(Future.succeededFuture(Saldo.ApiResponseSaldoDeleteAt.getDefaultInstance()));

    handler.restoreSaldo(ctx);
    verify(commandClient).restoreSaldo(any(Saldo.FindByIdSaldoRequest.class));
  }

  @Test
  void deleteSaldoPermanently() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "saldoId")).thenReturn(10);

    when(commandClient.deleteSaldoPermanent(any(Saldo.FindByIdSaldoRequest.class)))
        .thenReturn(Future.succeededFuture(SaldoCommand.ApiResponseSaldoDelete.getDefaultInstance()));

    handler.deleteSaldoPermanently(ctx);
    verify(commandClient).deleteSaldoPermanent(any(Saldo.FindByIdSaldoRequest.class));
  }

  @Test
  void restoreAllSaldos() {
    when(commandClient.restoreAllSaldo(any(com.google.protobuf.Empty.class)))
        .thenReturn(Future.succeededFuture(SaldoCommand.ApiResponseSaldoAll.getDefaultInstance()));

    handler.restoreAllSaldos(ctx);
    verify(commandClient).restoreAllSaldo(any(com.google.protobuf.Empty.class));
  }

  @Test
  void deleteAllPermanentSaldos() {
    when(commandClient.deleteAllSaldoPermanent(any(com.google.protobuf.Empty.class)))
        .thenReturn(Future.succeededFuture(SaldoCommand.ApiResponseSaldoAll.getDefaultInstance()));

    handler.deleteAllPermanentSaldos(ctx);
    verify(commandClient).deleteAllSaldoPermanent(any(com.google.protobuf.Empty.class));
  }

  @Test
  void getMonthlyTotalSaldoBalance() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("month"), anyInt())).thenReturn(1);

    when(totalStatsClient.findMonthlyTotalSaldoBalance(any(Saldo.FindMonthlySaldoTotalBalance.class)))
        .thenReturn(Future.succeededFuture(pb.saldo.stats.SaldoStatsTotal.ApiResponseMonthTotalSaldo.getDefaultInstance()));

    handler.getMonthlyTotalSaldoBalance(ctx);
    verify(totalStatsClient).findMonthlyTotalSaldoBalance(any(Saldo.FindMonthlySaldoTotalBalance.class));
  }

  @Test
  void getYearlyTotalSaldoBalances() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);

    when(totalStatsClient.findYearTotalSaldoBalance(any(Saldo.FindYearlySaldo.class)))
        .thenReturn(Future.succeededFuture(pb.saldo.stats.SaldoStatsTotal.ApiResponseYearTotalSaldo.getDefaultInstance()));

    handler.getYearlyTotalSaldoBalances(ctx);
    verify(totalStatsClient).findYearTotalSaldoBalance(any(Saldo.FindYearlySaldo.class));
  }

  @Test
  void getMonthlySaldoBalances() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);

    when(balanceStatsClient.findMonthlySaldoBalances(any(Saldo.FindYearlySaldo.class)))
        .thenReturn(Future.succeededFuture(pb.saldo.stats.SaldoStatsBalance.ApiResponseMonthSaldoBalances.getDefaultInstance()));

    handler.getMonthlySaldoBalances(ctx);
    verify(balanceStatsClient).findMonthlySaldoBalances(any(Saldo.FindYearlySaldo.class));
  }

  @Test
  void getYearlySaldoBalances() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);

    when(balanceStatsClient.findYearlySaldoBalances(any(Saldo.FindYearlySaldo.class)))
        .thenReturn(Future.succeededFuture(pb.saldo.stats.SaldoStatsBalance.ApiResponseYearSaldoBalances.getDefaultInstance()));

    handler.getYearlySaldoBalances(ctx);
    verify(balanceStatsClient).findYearlySaldoBalances(any(Saldo.FindYearlySaldo.class));
  }
}
