package io.example.saldo.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.saldo.domain.requests.MonthTotalSaldoBalance;
import io.example.saldo.service.SaldoStatsTotalService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.saldo.Saldo.FindMonthlySaldoTotalBalance;
import pb.saldo.Saldo.FindYearlySaldo;
import pb.saldo.stats.SaldoStatsTotal.ApiResponseMonthTotalSaldo;
import pb.saldo.stats.SaldoStatsTotal.ApiResponseYearTotalSaldo;

@RequiredArgsConstructor
public class SaldoStatsTotalHandler
    implements pb.saldo.stats.VertxSaldoStatsTotalBalanceGrpcServer.SaldoStatsTotalBalanceApi {
  private final SaldoStatsTotalService service;

  @Override
  public Future<ApiResponseMonthTotalSaldo> findMonthlyTotalSaldoBalance(FindMonthlySaldoTotalBalance req) {
    MonthTotalSaldoBalance domainReq = MonthTotalSaldoBalance.builder()
        .year(req.getYear())
        .month(req.getMonth())
        .build();

    return service.getMonthlyTotalSaldoBalance(domainReq)
        .map(res -> {
          var builder = ApiResponseMonthTotalSaldo.newBuilder()
              .setStatus("success")
              .setMessage("Monthly total assets fetched");
          res.stream().map(ProtoConverter::toProtoMonthTotal).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseYearTotalSaldo> findYearTotalSaldoBalance(FindYearlySaldo req) {
    return service.getYearlyTotalSaldoBalances(req.getYear())
        .map(res -> {
          var builder = ApiResponseYearTotalSaldo.newBuilder()
              .setStatus("success")
              .setMessage("Yearly total assets fetched");
          res.stream().map(ProtoConverter::toProtoYearTotal).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}