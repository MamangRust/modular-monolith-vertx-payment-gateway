package io.example.withdraw.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.withdraw.domain.requests.MonthStatusWithdrawCardNumber;
import io.example.withdraw.domain.requests.YearStatusWithdrawCardNumber;
import io.example.withdraw.service.WithdrawStatsStatusService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.withdraw.Withdraw.FindMonthlyWithdrawStatus;
import pb.withdraw.Withdraw.FindMonthlyWithdrawStatusCardNumber;
import pb.withdraw.Withdraw.FindYearWithdrawStatus;
import pb.withdraw.Withdraw.FindYearWithdrawStatusCardNumber;
import pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawMonthStatusFailed;
import pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawMonthStatusSuccess;
import pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawYearStatusFailed;
import pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawYearStatusSuccess;

@RequiredArgsConstructor
public class WithdrawStatsStatusHandler
    implements pb.withdraw.stats.VertxWithdrawStatsStatusServiceGrpcServer.WithdrawStatsStatusServiceApi {
  private final WithdrawStatsStatusService service;

  @Override
  public Future<ApiResponseWithdrawMonthStatusSuccess> findMonthlyWithdrawStatusSuccess(FindMonthlyWithdrawStatus req) {
    var domainReq = MonthStatusWithdrawCardNumber.builder()
        .year(req.getYear()).month(req.getMonth()).status("success").build();

    return service.getMonthlyWithdrawStatus(domainReq)
        .map(res -> ApiResponseWithdrawMonthStatusSuccess.newBuilder()
            .setStatus("success").setMessage("Monthly status success report generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawYearStatusSuccess> findYearlyWithdrawStatusSuccess(FindYearWithdrawStatus req) {
    var domainReq = YearStatusWithdrawCardNumber.builder()
        .year(req.getYear()).status("success").build();

    return service.getYearlyWithdrawStatus(domainReq)
        .map(res -> ApiResponseWithdrawYearStatusSuccess.newBuilder()
            .setStatus("success").setMessage("Yearly status success report generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawMonthStatusFailed> findMonthlyWithdrawStatusFailed(FindMonthlyWithdrawStatus req) {
    var domainReq = MonthStatusWithdrawCardNumber.builder()
        .year(req.getYear()).month(req.getMonth()).status("failed").build();

    return service.getMonthlyWithdrawStatus(domainReq)
        .map(res -> ApiResponseWithdrawMonthStatusFailed.newBuilder()
            .setStatus("success").setMessage("Monthly status failed report generated")
            .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawYearStatusFailed> findYearlyWithdrawStatusFailed(FindYearWithdrawStatus req) {
    var domainReq = YearStatusWithdrawCardNumber.builder()
        .year(req.getYear()).status("failed").build();

    return service.getYearlyWithdrawStatus(domainReq)
        .map(res -> ApiResponseWithdrawYearStatusFailed.newBuilder()
            .setStatus("success").setMessage("Yearly status failed report generated")
            .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawMonthStatusSuccess> findMonthlyWithdrawStatusSuccessCardNumber(
      FindMonthlyWithdrawStatusCardNumber req) {
    var domainReq = MonthStatusWithdrawCardNumber.builder()
        .cardNumber(req.getCardNumber()).year(req.getYear()).month(req.getMonth()).status("success").build();

    return service.getMonthlyStatusByCard(domainReq)
        .map(res -> ApiResponseWithdrawMonthStatusSuccess.newBuilder()
            .setStatus("success").setMessage("Monthly card status success report ready")
            .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawYearStatusSuccess> findYearlyWithdrawStatusSuccessCardNumber(
      FindYearWithdrawStatusCardNumber req) {
    var domainReq = YearStatusWithdrawCardNumber.builder()
        .cardNumber(req.getCardNumber()).year(req.getYear()).status("success").build();

    return service.getYearlyStatusByCard(domainReq)
        .map(res -> ApiResponseWithdrawYearStatusSuccess.newBuilder()
            .setStatus("success").setMessage("Yearly card status success report ready")
            .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawMonthStatusFailed> findMonthlyWithdrawStatusFailedCardNumber(
      FindMonthlyWithdrawStatusCardNumber req) {
    var domainReq = MonthStatusWithdrawCardNumber.builder()
        .cardNumber(req.getCardNumber()).year(req.getYear()).month(req.getMonth()).status("failed").build();

    return service.getMonthlyStatusByCard(domainReq)
        .map(res -> ApiResponseWithdrawMonthStatusFailed.newBuilder()
            .setStatus("success").setMessage("Monthly card status failed report ready")
            .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawYearStatusFailed> findYearlyWithdrawStatusFailedCardNumber(
      FindYearWithdrawStatusCardNumber req) {
    var domainReq = YearStatusWithdrawCardNumber.builder()
        .cardNumber(req.getCardNumber()).year(req.getYear()).status("failed").build();

    return service.getYearlyStatusByCard(domainReq)
        .map(res -> ApiResponseWithdrawYearStatusFailed.newBuilder()
            .setStatus("success").setMessage("Yearly card status failed report ready")
            .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList()).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}