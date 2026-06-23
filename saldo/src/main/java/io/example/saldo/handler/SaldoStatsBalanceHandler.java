package io.example.saldo.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.saldo.service.SaldoStatsBalanceService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.saldo.Saldo.FindYearlySaldo;
import pb.saldo.stats.SaldoStatsBalance.ApiResponseMonthSaldoBalances;
import pb.saldo.stats.SaldoStatsBalance.ApiResponseYearSaldoBalances;

@RequiredArgsConstructor
public class SaldoStatsBalanceHandler
    implements pb.saldo.stats.VertxSaldoStatsBalanceServiceGrpcServer.SaldoStatsBalanceServiceApi {
  private final SaldoStatsBalanceService service;

  @Override
  public Future<ApiResponseMonthSaldoBalances> findMonthlySaldoBalances(FindYearlySaldo req) {
    return service.getMonthlySaldoBalances(req.getYear())
        .map(res -> {
          var builder = ApiResponseMonthSaldoBalances.newBuilder()
              .setStatus("success")
              .setMessage("Monthly ledger balances retrieved");
          res.stream().map(ProtoConverter::toProtoMonthBal).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseYearSaldoBalances> findYearlySaldoBalances(FindYearlySaldo req) {
    return service.getYearlySaldoBalances(req.getYear())
        .map(res -> {
          var builder = ApiResponseYearSaldoBalances.newBuilder()
              .setStatus("success")
              .setMessage("Yearly ledger balances retrieved");
          res.stream().map(ProtoConverter::toProtoYearBal).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}