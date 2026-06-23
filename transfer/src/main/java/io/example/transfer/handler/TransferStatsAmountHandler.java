package io.example.transfer.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transfer.domain.requests.MonthYearCardNumber;
import io.example.transfer.service.TransferStatsAmountService;
import io.example.transfer.service.TransferStatsByCardAmountService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transfer.Transfer.FindByCardNumberTransferRequest;
import pb.transfer.Transfer.FindYearTransferStatus;
import pb.transfer.stats.TransferStatsAmount.ApiResponseTransferMonthAmount;
import pb.transfer.stats.TransferStatsAmount.ApiResponseTransferYearAmount;

@RequiredArgsConstructor
public class TransferStatsAmountHandler
    implements pb.transfer.stats.VertxTransferStatsAmountServiceGrpcServer.TransferStatsAmountServiceApi {
  private final TransferStatsAmountService service;
  private final TransferStatsByCardAmountService byCardService;

  @Override
  public Future<ApiResponseTransferMonthAmount> findMonthlyTransferAmounts(FindYearTransferStatus req) {
    return service.getMonthlyTransferAmounts(req.getYear())
        .map(res -> ApiResponseTransferMonthAmount.newBuilder()
            .setStatus("success").setMessage("Monthly transfers volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferYearAmount> findYearlyTransferAmounts(FindYearTransferStatus req) {
    return service.getYearlyTransferAmounts(req.getYear())
        .map(res -> ApiResponseTransferYearAmount.newBuilder()
            .setStatus("success").setMessage("Yearly transfers volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferMonthAmount> findMonthlyTransferAmountsBySenderCardNumber(
      FindByCardNumberTransferRequest req) {
    var domainReq = MonthYearCardNumber.builder().cardNumber(req.getCardNumber()).year(req.getYear()).build();
    return byCardService.getMonthlySenderAmountsByCard(domainReq)
        .map(res -> ApiResponseTransferMonthAmount.newBuilder()
            .setStatus("success").setMessage("Monthly sender transfer volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferMonthAmount> findMonthlyTransferAmountsByReceiverCardNumber(
      FindByCardNumberTransferRequest req) {
    var domainReq = MonthYearCardNumber.builder().cardNumber(req.getCardNumber()).year(req.getYear()).build();
    return byCardService.getMonthlyReceiverAmountsByCard(domainReq)
        .map(res -> ApiResponseTransferMonthAmount.newBuilder()
            .setStatus("success").setMessage("Monthly receiver transfer volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferYearAmount> findYearlyTransferAmountsBySenderCardNumber(
      FindByCardNumberTransferRequest req) {
    var domainReq = MonthYearCardNumber.builder().cardNumber(req.getCardNumber()).year(req.getYear()).build();
    return byCardService.getYearlySenderAmountsByCard(domainReq)
        .map(res -> ApiResponseTransferYearAmount.newBuilder()
            .setStatus("success").setMessage("Yearly sender transfer volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferYearAmount> findYearlyTransferAmountsByReceiverCardNumber(
      FindByCardNumberTransferRequest req) {
    var domainReq = MonthYearCardNumber.builder().cardNumber(req.getCardNumber()).year(req.getYear()).build();
    return byCardService.getYearlyReceiverAmountsByCard(domainReq)
        .map(res -> ApiResponseTransferYearAmount.newBuilder()
            .setStatus("success").setMessage("Yearly receiver transfer volume retrieved")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}