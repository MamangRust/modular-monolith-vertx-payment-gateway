package io.example.transfer.handler;

import io.example.transfer.service.TransferStatsStatusService;
import io.vertx.core.Future;
import pb.transfer.Transfer.FindMonthlyTransferStatus;
import pb.transfer.Transfer.FindMonthlyTransferStatusCardNumber;
import pb.transfer.Transfer.FindYearTransferStatus;
import pb.transfer.Transfer.FindYearTransferStatusCardNumber;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferMonthStatusFailed;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferMonthStatusSuccess;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferYearStatusFailed;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferYearStatusSuccess;

public class TransferStatsStatusHandler
    implements pb.transfer.stats.VertxTransferStatsStatusServiceGrpcServer.TransferStatsStatusServiceApi {
  private final TransferStatsStatusService service;
  private final io.example.transfer.service.TransferStatsByCardStatusService byCardService;

  public TransferStatsStatusHandler(TransferStatsStatusService service, io.example.transfer.service.TransferStatsByCardStatusService byCardService) {
    this.service = service;
    this.byCardService = byCardService;
  }

  @Override
  public Future<ApiResponseTransferMonthStatusSuccess> findMonthlyTransferStatusSuccess(FindMonthlyTransferStatus req) {
    return service.getMonthlyTransferStatus(req, "success")
        .map(res -> ApiResponseTransferMonthStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Monthly transfer status success stats generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferYearStatusSuccess> findYearlyTransferStatusSuccess(
      FindYearTransferStatus req) {
    return service.getYearlyTransferStatus(req, "success")
        .map(res -> ApiResponseTransferYearStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Yearly transfer status success stats generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferMonthStatusFailed> findMonthlyTransferStatusFailed(FindMonthlyTransferStatus req) {
    return service.getMonthlyTransferStatus(req, "failed")
        .map(res -> ApiResponseTransferMonthStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Monthly transfer status failed stats generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferYearStatusFailed> findYearlyTransferStatusFailed(
      pb.transfer.Transfer.FindYearTransferStatus req) {
    return service.getYearlyTransferStatus(req, "failed")
        .map(res -> ApiResponseTransferYearStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Yearly transfer status failed stats generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferMonthStatusSuccess> findMonthlyTransferStatusSuccessByCardNumber(
      FindMonthlyTransferStatusCardNumber req) {
    return byCardService.getMonthlyStatusByCard(req, "success")
        .map(res -> ApiResponseTransferMonthStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Monthly transfer status success by card stats generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferYearStatusSuccess> findYearlyTransferStatusSuccessByCardNumber(
      FindYearTransferStatusCardNumber req) {
    return byCardService.getYearlyStatusByCard(req, "success")
        .map(res -> ApiResponseTransferYearStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Yearly transfer status success by card stats generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferMonthStatusFailed> findMonthlyTransferStatusFailedByCardNumber(
      FindMonthlyTransferStatusCardNumber req) {
    return byCardService.getMonthlyStatusByCard(req, "failed")
        .map(res -> ApiResponseTransferMonthStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Monthly transfer status failed by card stats generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTransferYearStatusFailed> findYearlyTransferStatusFailedByCardNumber(
      FindYearTransferStatusCardNumber req) {
    return byCardService.getYearlyStatusByCard(req, "failed")
        .map(res -> ApiResponseTransferYearStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Yearly transfer status failed by card stats generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList())
            .build());
  }
}
