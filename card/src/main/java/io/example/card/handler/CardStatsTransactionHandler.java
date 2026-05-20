package io.example.card.handler;

import io.example.card.service.CardStatsTransactionService;
import io.vertx.core.Future;
import pb.card.Card.*;

public class CardStatsTransactionHandler implements pb.card.stats.VertxCardStatsTransactionServiceGrpcServer.CardStatsTransactionServiceApi {
  private final CardStatsTransactionService service;

  public CardStatsTransactionHandler(CardStatsTransactionService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseMonthlyAmount> findMonthlyTransactionAmount(FindYearAmount req) {
    return service.getMonthlyTransactionAmount(req.getYear())
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
  public Future<ApiResponseYearlyAmount> findYearlyTransactionAmount(FindYearAmount req) {
    return service.getYearlyTransactionAmount(req.getYear())
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
  public Future<ApiResponseMonthlyAmount> findMonthlyTransactionAmountByCardNumber(FindYearAmountCardNumber req) {
    return service.getMonthlyTransactionAmountByCardNumber(req.getYear(), req.getCardNumber())
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
  public Future<ApiResponseYearlyAmount> findYearlyTransactionAmountByCardNumber(FindYearAmountCardNumber req) {
    return service.getYearlyTransactionAmountByCardNumber(req.getYear(), req.getCardNumber())
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
