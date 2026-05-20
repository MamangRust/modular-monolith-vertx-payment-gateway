package io.example.transfer.handler;

import io.example.transfer.service.TransferStatsAmountService;
import io.vertx.core.Future;
import pb.transfer.Transfer.*;
import pb.transfer.stats.TransferStatsAmount.*;

public class TransferStatsAmountHandler implements pb.transfer.stats.VertxTransferStatsAmountServiceGrpcServer.TransferStatsAmountServiceApi {
  private final TransferStatsAmountService service;
  private final io.example.transfer.service.TransferStatsByCardAmountService byCardService;

  public TransferStatsAmountHandler(TransferStatsAmountService service, io.example.transfer.service.TransferStatsByCardAmountService byCardService) {
    this.service = service;
    this.byCardService = byCardService;
  }

  @Override
  public Future<ApiResponseTransferMonthAmount> findMonthlyTransferAmounts(FindYearTransferStatus req) {
    return service.getMonthlyTransferAmounts(req.getYear())
        .map(res -> ApiResponseTransferMonthAmount.newBuilder()
            .setStatus("success")
            .setMessage("Monthly transfers volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferYearAmount> findYearlyTransferAmounts(FindYearTransferStatus req) {
    return service.getYearlyTransferAmounts(req.getYear())
        .map(res -> ApiResponseTransferYearAmount.newBuilder()
            .setStatus("success")
            .setMessage("Yearly transfers volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferMonthAmount> findMonthlyTransferAmountsBySenderCardNumber(FindByCardNumberTransferRequest req) {
    return byCardService.getMonthlySenderAmountsByCard(req.getCardNumber(), req.getYear())
        .map(res -> ApiResponseTransferMonthAmount.newBuilder()
            .setStatus("success")
            .setMessage("Monthly sender transfer volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferMonthAmount> findMonthlyTransferAmountsByReceiverCardNumber(FindByCardNumberTransferRequest req) {
    return byCardService.getMonthlyReceiverAmountsByCard(req.getCardNumber(), req.getYear())
        .map(res -> ApiResponseTransferMonthAmount.newBuilder()
            .setStatus("success")
            .setMessage("Monthly receiver transfer volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferYearAmount> findYearlyTransferAmountsBySenderCardNumber(FindByCardNumberTransferRequest req) {
    return byCardService.getYearlySenderAmountsByCard(req.getCardNumber(), req.getYear())
        .map(res -> ApiResponseTransferYearAmount.newBuilder()
            .setStatus("success")
            .setMessage("Yearly sender transfer volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferYearAmount> findYearlyTransferAmountsByReceiverCardNumber(FindByCardNumberTransferRequest req) {
    return byCardService.getYearlyReceiverAmountsByCard(req.getCardNumber(), req.getYear())
        .map(res -> ApiResponseTransferYearAmount.newBuilder()
            .setStatus("success")
            .setMessage("Yearly receiver transfer volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList())
            .build());
  }
}
