package io.example.withdraw.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.withdraw.domain.requests.YearMonthCardNumber;
import io.example.withdraw.service.WithdrawStatsAmountService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.withdraw.Withdraw.FindYearWithdrawCardNumber;
import pb.withdraw.Withdraw.FindYearWithdrawStatus;
import pb.withdraw.stats.WithdrawStatsAmount.ApiResponseWithdrawMonthAmount;
import pb.withdraw.stats.WithdrawStatsAmount.ApiResponseWithdrawYearAmount;

@RequiredArgsConstructor
public class WithdrawStatsAmountHandler
    implements pb.withdraw.stats.VertxWithdrawStatsAmountServiceGrpcServer.WithdrawStatsAmountServiceApi {
  private final WithdrawStatsAmountService service;

  @Override
  public Future<ApiResponseWithdrawMonthAmount> findMonthlyWithdraws(FindYearWithdrawStatus req) {
    return service.getMonthlyWithdrawAmounts(req.getYear())
        .map(res -> ApiResponseWithdrawMonthAmount.newBuilder()
            .setStatus("success")
            .setMessage("Monthly withdrawals report ready")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawYearAmount> findYearlyWithdraws(FindYearWithdrawStatus req) {
    return service.getYearlyWithdrawAmounts(req.getYear())
        .map(res -> ApiResponseWithdrawYearAmount.newBuilder()
            .setStatus("success")
            .setMessage("Yearly withdrawals report ready")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawMonthAmount> findMonthlyWithdrawsByCardNumber(FindYearWithdrawCardNumber req) {
    var domainReq = YearMonthCardNumber.builder()
        .cardNumber(req.getCardNumber())
        .year(req.getYear())
        .build();

    return service.getMonthlyWithdrawAmountsByCard(domainReq)
        .map(res -> ApiResponseWithdrawMonthAmount.newBuilder()
            .setStatus("success")
            .setMessage("Monthly card withdrawals report ready")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawYearAmount> findYearlyWithdrawsByCardNumber(FindYearWithdrawCardNumber req) {
    var domainReq = YearMonthCardNumber.builder()
        .cardNumber(req.getCardNumber())
        .year(req.getYear())
        .build();

    return service.getYearlyWithdrawAmountsByCard(domainReq)
        .map(res -> ApiResponseWithdrawYearAmount.newBuilder()
            .setStatus("success")
            .setMessage("Yearly card withdrawals report ready")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}