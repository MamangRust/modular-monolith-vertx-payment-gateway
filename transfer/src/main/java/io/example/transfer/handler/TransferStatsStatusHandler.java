package io.example.transfer.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transfer.domain.requests.MonthStatusTransfer;
import io.example.transfer.domain.requests.MonthStatusTransferCardNumber;
import io.example.transfer.domain.requests.YearStatusTransferCardNumber;
import io.example.transfer.domain.requests.YearStatusTransferRequest;
import io.example.transfer.service.TransferStatsByCardStatusService;
import io.example.transfer.service.TransferStatsStatusService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transfer.Transfer.FindMonthlyTransferStatus;
import pb.transfer.Transfer.FindMonthlyTransferStatusCardNumber;
import pb.transfer.Transfer.FindYearTransferStatus;
import pb.transfer.Transfer.FindYearTransferStatusCardNumber;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferMonthStatusFailed;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferMonthStatusSuccess;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferYearStatusFailed;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferYearStatusSuccess;

@RequiredArgsConstructor
public class TransferStatsStatusHandler
    implements pb.transfer.stats.VertxTransferStatsStatusServiceGrpcServer.TransferStatsStatusServiceApi {
  private final TransferStatsStatusService service;
  private final TransferStatsByCardStatusService byCardService;

  @Override
  public Future<ApiResponseTransferMonthStatusSuccess> findMonthlyTransferStatusSuccess(FindMonthlyTransferStatus req) {
    var domainReq = MonthStatusTransfer.builder().year(req.getYear()).month(req.getMonth()).status("success").build();
    return service.getMonthlyTransferStatus(domainReq)
        .map(res -> ApiResponseTransferMonthStatusSuccess.newBuilder()
            .setStatus("success").setMessage("Monthly transfer status success stats generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferYearStatusSuccess> findYearlyTransferStatusSuccess(FindYearTransferStatus req) {
    var domainReq = YearStatusTransferRequest.builder().year(req.getYear()).status("success").build();
    return service.getYearlyTransferStatus(domainReq)
        .map(res -> ApiResponseTransferYearStatusSuccess.newBuilder()
            .setStatus("success").setMessage("Yearly transfer status success stats generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferMonthStatusFailed> findMonthlyTransferStatusFailed(FindMonthlyTransferStatus req) {
    var domainReq = MonthStatusTransfer.builder().year(req.getYear()).month(req.getMonth()).status("failed").build();
    return service.getMonthlyTransferStatus(domainReq)
        .map(res -> ApiResponseTransferMonthStatusFailed.newBuilder()
            .setStatus("success").setMessage("Monthly transfer status failed stats generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferYearStatusFailed> findYearlyTransferStatusFailed(FindYearTransferStatus req) {
    var domainReq = YearStatusTransferRequest.builder().year(req.getYear()).status("failed").build();
    return service.getYearlyTransferStatus(domainReq)
        .map(res -> ApiResponseTransferYearStatusFailed.newBuilder()
            .setStatus("success").setMessage("Yearly transfer status failed stats generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferMonthStatusSuccess> findMonthlyTransferStatusSuccessByCardNumber(
      FindMonthlyTransferStatusCardNumber req) {
    var domainReq = MonthStatusTransferCardNumber.builder()
        .cardNumber(req.getCardNumber()).year(req.getYear()).month(req.getMonth()).status("success").build();
    return byCardService.getMonthlyStatusByCard(domainReq)
        .map(res -> ApiResponseTransferMonthStatusSuccess.newBuilder()
            .setStatus("success").setMessage("Monthly transfer status success by card stats generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferYearStatusSuccess> findYearlyTransferStatusSuccessByCardNumber(
      FindYearTransferStatusCardNumber req) {
    var domainReq = YearStatusTransferCardNumber.builder()
        .cardNumber(req.getCardNumber()).year(req.getYear()).status("success").build();
    return byCardService.getYearlyStatusByCard(domainReq)
        .map(res -> ApiResponseTransferYearStatusSuccess.newBuilder()
            .setStatus("success").setMessage("Yearly transfer status success by card stats generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferMonthStatusFailed> findMonthlyTransferStatusFailedByCardNumber(
      FindMonthlyTransferStatusCardNumber req) {
    var domainReq = MonthStatusTransferCardNumber.builder()
        .cardNumber(req.getCardNumber()).year(req.getYear()).month(req.getMonth()).status("failed").build();
    return byCardService.getMonthlyStatusByCard(domainReq)
        .map(res -> ApiResponseTransferMonthStatusFailed.newBuilder()
            .setStatus("success").setMessage("Monthly transfer status failed by card stats generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferYearStatusFailed> findYearlyTransferStatusFailedByCardNumber(
      FindYearTransferStatusCardNumber req) {
    var domainReq = YearStatusTransferCardNumber.builder()
        .cardNumber(req.getCardNumber()).year(req.getYear()).status("failed").build();
    return byCardService.getYearlyStatusByCard(domainReq)
        .map(res -> ApiResponseTransferYearStatusFailed.newBuilder()
            .setStatus("success").setMessage("Yearly transfer status failed by card stats generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}