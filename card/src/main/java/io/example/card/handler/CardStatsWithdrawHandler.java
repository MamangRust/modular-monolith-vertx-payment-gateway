package io.example.card.handler;

import io.example.card.service.CardStatsWithdrawService;
import io.vertx.core.Future;
import pb.card.Card.*;

public class CardStatsWithdrawHandler implements pb.card.stats.VertxCardStatsWithdrawServiceGrpcServer.CardStatsWithdrawServiceApi {
  private final CardStatsWithdrawService service;

  public CardStatsWithdrawHandler(CardStatsWithdrawService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseMonthlyAmount> findMonthlyWithdrawAmount(FindYearAmount req) {
    return service.getMonthlyWithdrawAmount(req.getYear())
        .map(res -> ApiResponseMonthlyAmount.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(d -> CardResponseMonthlyAmount.newBuilder()
                .setMonth(String.valueOf(d.getMonth()))
                .setTotalAmount(d.getAmount())
                .build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseYearlyAmount> findYearlyWithdrawAmount(FindYearAmount req) {
    return service.getYearlyWithdrawAmount(req.getYear())
        .map(res -> ApiResponseYearlyAmount.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(d -> CardResponseYearlyAmount.newBuilder()
                .setYear(String.valueOf(d.getYear()))
                .setTotalAmount(d.getAmount())
                .build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMonthlyAmount> findMonthlyWithdrawAmountByCardNumber(FindYearAmountCardNumber req) {
    return service.getMonthlyWithdrawAmountByCardNumber(req.getYear(), req.getCardNumber())
        .map(res -> ApiResponseMonthlyAmount.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(d -> CardResponseMonthlyAmount.newBuilder()
                .setMonth(String.valueOf(d.getMonth()))
                .setTotalAmount(d.getAmount())
                .build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseYearlyAmount> findYearlyWithdrawAmountByCardNumber(FindYearAmountCardNumber req) {
    return service.getYearlyWithdrawAmountByCardNumber(req.getYear(), req.getCardNumber())
        .map(res -> ApiResponseYearlyAmount.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(d -> CardResponseYearlyAmount.newBuilder()
                .setYear(String.valueOf(d.getYear()))
                .setTotalAmount(d.getAmount())
                .build()).toList())
            .build());
  }
}
