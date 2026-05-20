package io.example.topup.handler;

import io.example.topup.service.TopupStatsStatusService;
import io.vertx.core.Future;
import pb.topup.Topup.*;
import pb.topup.stats.TopupStatsStatus.*;

public class TopupStatsStatusHandler
    implements pb.topup.stats.VertxTopupStatsStatusServiceGrpcServer.TopupStatsStatusServiceApi {
  private final TopupStatsStatusService service;

  public TopupStatsStatusHandler(TopupStatsStatusService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseTopupMonthStatusSuccess> findMonthlyTopupStatusSuccess(FindMonthlyTopupStatus req) {
    return service.getMonthlyTopupStatus(req, "success")
        .map(res -> ApiResponseTopupMonthStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Monthly success topup counts aggregated")
            .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupYearStatusSuccess> findYearlyTopupStatusSuccess(FindYearTopupStatus req) {
    return service.getYearlyTopupStatus(req, "success")
        .map(res -> ApiResponseTopupYearStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Yearly success topup counts aggregated")
            .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupMonthStatusFailed> findMonthlyTopupStatusFailed(FindMonthlyTopupStatus req) {
    return service.getMonthlyTopupStatus(req, "failed")
        .map(res -> ApiResponseTopupMonthStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Monthly failed topup counts aggregated")
            .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupYearStatusFailed> findYearlyTopupStatusFailed(FindYearTopupStatus req) {
    return service.getYearlyTopupStatus(req, "failed")
        .map(res -> ApiResponseTopupYearStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Yearly failed topup counts aggregated")
            .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupMonthStatusSuccess> findMonthlyTopupStatusSuccessByCardNumber(
      FindMonthlyTopupStatusCardNumber req) {
    return service.getMonthlyTopupStatusByCard(req, "success")
        .map(res -> ApiResponseTopupMonthStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Monthly card success topups computed")
            .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupYearStatusSuccess> findYearlyTopupStatusSuccessByCardNumber(
      FindYearTopupStatusCardNumber req) {
    return service.getYearlyTopupStatusByCard(req, "success")
        .map(res -> ApiResponseTopupYearStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Yearly card success topups computed")
            .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupMonthStatusFailed> findMonthlyTopupStatusFailedByCardNumber(
      FindMonthlyTopupStatusCardNumber req) {
    return service.getMonthlyTopupStatusByCard(req, "failed")
        .map(res -> ApiResponseTopupMonthStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Monthly card failed topups computed")
            .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupYearStatusFailed> findYearlyTopupStatusFailedByCardNumber(
      FindYearTopupStatusCardNumber req) {
    return service.getYearlyTopupStatusByCard(req, "failed")
        .map(res -> ApiResponseTopupYearStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Yearly card failed topups computed")
            .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList())
            .build());
  }
}
