package io.example.card.handler;

import io.example.card.service.CardStatsTopupService;
import io.vertx.core.Future;
import pb.card.Card.*;

public class CardStatsTopupHandler implements pb.card.stats.VertxCardStatsTopupServiceGrpcServer.CardStatsTopupServiceApi {
  private final CardStatsTopupService service;

  public CardStatsTopupHandler(CardStatsTopupService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseMonthlyAmount> findMonthlyTopupAmount(FindYearAmount req) {
    return service.getMonthlyTopupAmount(req.getYear())
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
  public Future<ApiResponseYearlyAmount> findYearlyTopupAmount(FindYearAmount req) {
    return service.getYearlyTopupAmount(req.getYear())
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
  public Future<ApiResponseMonthlyAmount> findMonthlyTopupAmountByCardNumber(FindYearAmountCardNumber req) {
    return service.getMonthlyTopupAmountByCardNumber(req.getYear(), req.getCardNumber())
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
  public Future<ApiResponseYearlyAmount> findYearlyTopupAmountByCardNumber(FindYearAmountCardNumber req) {
    return service.getYearlyTopupAmountByCardNumber(req.getYear(), req.getCardNumber())
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
