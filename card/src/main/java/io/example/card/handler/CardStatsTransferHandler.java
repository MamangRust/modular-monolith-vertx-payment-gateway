package io.example.card.handler;

import io.example.card.service.CardStatsTransferService;
import io.vertx.core.Future;
import pb.card.Card.*;

public class CardStatsTransferHandler implements pb.card.stats.VertxCardStatsTransferServiceGrpcServer.CardStatsTransferServiceApi {
  private final CardStatsTransferService service;

  public CardStatsTransferHandler(CardStatsTransferService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseMonthlyAmount> findMonthlyTransferSenderAmount(FindYearAmount req) {
    return service.getMonthlyTransferAmountSender(req.getYear())
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
  public Future<ApiResponseMonthlyAmount> findMonthlyTransferReceiverAmount(FindYearAmount req) {
    return service.getMonthlyTransferAmountReceiver(req.getYear())
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
  public Future<ApiResponseYearlyAmount> findYearlyTransferSenderAmount(FindYearAmount req) {
    return service.getYearlyTransferAmountSender(req.getYear())
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
  public Future<ApiResponseYearlyAmount> findYearlyTransferReceiverAmount(FindYearAmount req) {
    return service.getYearlyTransferAmountReceiver(req.getYear())
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
  public Future<ApiResponseMonthlyAmount> findMonthlyTransferSenderAmountByCardNumber(FindYearAmountCardNumber req) {
    return service.getMonthlyTransferAmountBySender(req.getYear(), req.getCardNumber())
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
  public Future<ApiResponseMonthlyAmount> findMonthlyTransferReceiverAmountByCardNumber(FindYearAmountCardNumber req) {
    return service.getMonthlyTransferAmountByReceiver(req.getYear(), req.getCardNumber())
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
  public Future<ApiResponseYearlyAmount> findYearlyTransferSenderAmountByCardNumber(FindYearAmountCardNumber req) {
    return service.getYearlyTransferAmountBySender(req.getYear(), req.getCardNumber())
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
  public Future<ApiResponseYearlyAmount> findYearlyTransferReceiverAmountByCardNumber(FindYearAmountCardNumber req) {
    return service.getYearlyTransferAmountByReceiver(req.getYear(), req.getCardNumber())
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
