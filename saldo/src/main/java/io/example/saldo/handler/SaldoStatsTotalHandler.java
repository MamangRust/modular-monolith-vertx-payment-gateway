package io.example.saldo.handler;

import io.example.saldo.domain.requests.MonthTotalSaldoBalance;
import io.example.saldo.service.SaldoStatsTotalService;
import io.vertx.core.Future;
import pb.saldo.Saldo.FindMonthlySaldoTotalBalance;
import pb.saldo.Saldo.FindYearlySaldo;
import pb.saldo.stats.SaldoStatsTotal.ApiResponseMonthTotalSaldo;
import pb.saldo.stats.SaldoStatsTotal.ApiResponseYearTotalSaldo;

public class SaldoStatsTotalHandler
    implements pb.saldo.stats.VertxSaldoStatsTotalBalanceGrpcServer.SaldoStatsTotalBalanceApi {
  private final SaldoStatsTotalService service;

  public SaldoStatsTotalHandler(SaldoStatsTotalService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseMonthTotalSaldo> findMonthlyTotalSaldoBalance(FindMonthlySaldoTotalBalance req) {
    MonthTotalSaldoBalance domainReq = MonthTotalSaldoBalance.builder()
        .year(req.getYear())
        .month(req.getMonth())
        .build();

    return service.getMonthlyTotalSaldoBalance(domainReq)
        .map(res -> ApiResponseMonthTotalSaldo.newBuilder()
            .setStatus("success")
            .setMessage("Monthly total assets fetched")
            .addAllData(res.stream().map(ProtoConverter::toProtoMonthTotal).toList())
            .build());
  }

  @Override
  public Future<ApiResponseYearTotalSaldo> findYearTotalSaldoBalance(FindYearlySaldo req) {
    return service.getYearlyTotalSaldoBalances(req.getYear())
        .map(res -> ApiResponseYearTotalSaldo.newBuilder()
            .setStatus("success")
            .setMessage("Yearly total assets fetched")
            .addAllData(res.stream().map(ProtoConverter::toProtoYearTotal).toList())
            .build());
  }
}
