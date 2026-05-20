package io.example.card.handler;

import io.example.card.service.CardStatsBalanceService;
import io.vertx.core.Future;
import pb.card.stats.CardStatsBalance.*;

public class CardStatsBalanceHandler implements pb.card.stats.VertxCardStatsBalanceServiceGrpcServer.CardStatsBalanceServiceApi {
  private final CardStatsBalanceService service;

  public CardStatsBalanceHandler(CardStatsBalanceService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseMonthlyBalance> findMonthlyBalance(FindYearBalance req) {
    return service.getMonthlyBalances(req.getYear())
        .map(res -> ApiResponseMonthlyBalance.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(d -> CardResponseMonthlyBalance.newBuilder()
                .setMonth(String.valueOf(d.getMonth()))
                .setTotalBalance(d.getBalance())
                .build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseYearlyBalance> findYearlyBalance(FindYearBalance req) {
    return service.getYearlyBalances(req.getYear())
        .map(res -> ApiResponseYearlyBalance.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(d -> CardResponseYearlyBalance.newBuilder()
                .setYear(String.valueOf(d.getYear()))
                .setTotalBalance(d.getBalance())
                .build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMonthlyBalance> findMonthlyBalanceByCardNumber(FindYearBalanceCardNumber req) {
    return service.getMonthlyBalancesByCardNumber(req.getYear(), req.getCardNumber())
        .map(res -> ApiResponseMonthlyBalance.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(d -> CardResponseMonthlyBalance.newBuilder()
                .setMonth(String.valueOf(d.getMonth()))
                .setTotalBalance(d.getBalance())
                .build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseYearlyBalance> findYearlyBalanceByCardNumber(FindYearBalanceCardNumber req) {
    return service.getYearlyBalancesByCardNumber(req.getYear(), req.getCardNumber())
        .map(res -> ApiResponseYearlyBalance.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(d -> CardResponseYearlyBalance.newBuilder()
                .setYear(String.valueOf(d.getYear()))
                .setTotalBalance(d.getBalance())
                .build()).toList())
            .build());
  }
}
