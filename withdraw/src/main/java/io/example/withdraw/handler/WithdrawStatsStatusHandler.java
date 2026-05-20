package io.example.withdraw.handler;

import io.example.withdraw.service.WithdrawStatsStatusService;
import io.vertx.core.Future;
import pb.withdraw.Withdraw.*;
import pb.withdraw.stats.WithdrawStatsStatus.*;

public class WithdrawStatsStatusHandler
    implements pb.withdraw.stats.VertxWithdrawStatsStatusServiceGrpcServer.WithdrawStatsStatusServiceApi {
  private final WithdrawStatsStatusService service;

  public WithdrawStatsStatusHandler(WithdrawStatsStatusService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseWithdrawMonthStatusSuccess> findMonthlyWithdrawStatusSuccess(FindMonthlyWithdrawStatus req) {
    return service.getMonthlyWithdrawStatus(req.getYear(), req.getMonth(), "success")
        .map(res -> ApiResponseWithdrawMonthStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Monthly status success report generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawYearStatusSuccess> findYearlyWithdrawStatusSuccess(FindYearWithdrawStatus req) {
    return service.getYearlyWithdrawStatus(req.getYear(), "success")
        .map(res -> ApiResponseWithdrawYearStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Yearly status success report generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawMonthStatusFailed> findMonthlyWithdrawStatusFailed(FindMonthlyWithdrawStatus req) {
    return service.getMonthlyWithdrawStatus(req.getYear(), req.getMonth(), "failed")
        .map(res -> ApiResponseWithdrawMonthStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Monthly status failed report generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawYearStatusFailed> findYearlyWithdrawStatusFailed(FindYearWithdrawStatus req) {
    return service.getYearlyWithdrawStatus(req.getYear(), "failed")
        .map(res -> ApiResponseWithdrawYearStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Yearly status failed report generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawMonthStatusSuccess> findMonthlyWithdrawStatusSuccessCardNumber(
      FindMonthlyWithdrawStatusCardNumber req) {
    return service.getMonthlyStatusByCard(req.getCardNumber(), req.getYear(), req.getMonth(), "success")
        .map(res -> ApiResponseWithdrawMonthStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Monthly card status success report ready")
            .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawYearStatusSuccess> findYearlyWithdrawStatusSuccessCardNumber(
      FindYearWithdrawStatusCardNumber req) {
    return service.getYearlyStatusByCard(req.getCardNumber(), req.getYear(), "success")
        .map(res -> ApiResponseWithdrawYearStatusSuccess.newBuilder()
            .setStatus("success")
            .setMessage("Yearly card status success report ready")
            .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawMonthStatusFailed> findMonthlyWithdrawStatusFailedCardNumber(
      FindMonthlyWithdrawStatusCardNumber req) {
    return service.getMonthlyStatusByCard(req.getCardNumber(), req.getYear(), req.getMonth(), "failed")
        .map(res -> ApiResponseWithdrawMonthStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Monthly card status failed report ready")
            .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawYearStatusFailed> findYearlyWithdrawStatusFailedCardNumber(
      FindYearWithdrawStatusCardNumber req) {
    return service.getYearlyStatusByCard(req.getCardNumber(), req.getYear(), "failed")
        .map(res -> ApiResponseWithdrawYearStatusFailed.newBuilder()
            .setStatus("success")
            .setMessage("Yearly card status failed report ready")
            .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList())
            .build());
  }
}
