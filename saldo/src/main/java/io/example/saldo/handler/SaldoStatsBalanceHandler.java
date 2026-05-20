package io.example.saldo.handler;

import io.example.saldo.service.SaldoStatsBalanceService;
import io.vertx.core.Future;
import pb.saldo.Saldo.FindYearlySaldo;
import pb.saldo.stats.SaldoStatsBalance.ApiResponseMonthSaldoBalances;
import pb.saldo.stats.SaldoStatsBalance.ApiResponseYearSaldoBalances;

public class SaldoStatsBalanceHandler
    implements pb.saldo.stats.VertxSaldoStatsBalanceServiceGrpcServer.SaldoStatsBalanceServiceApi {
  private final SaldoStatsBalanceService service;

  public SaldoStatsBalanceHandler(SaldoStatsBalanceService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseMonthSaldoBalances> findMonthlySaldoBalances(FindYearlySaldo req) {
    return service.getMonthlySaldoBalances(req.getYear())
        .map(res -> ApiResponseMonthSaldoBalances.newBuilder()
            .setStatus("success")
            .setMessage("Monthly ledger balances retrieved")
            .addAllData(res.stream().map(ProtoConverter::toProtoMonthBal).toList())
            .build());
  }

  @Override
  public Future<ApiResponseYearSaldoBalances> findYearlySaldoBalances(FindYearlySaldo req) {
    return service.getYearlySaldoBalances(req.getYear())
        .map(res -> ApiResponseYearSaldoBalances.newBuilder()
            .setStatus("success")
            .setMessage("Yearly ledger balances retrieved")
            .addAllData(res.stream().map(ProtoConverter::toProtoYearBal).toList())
            .build());
  }
}
