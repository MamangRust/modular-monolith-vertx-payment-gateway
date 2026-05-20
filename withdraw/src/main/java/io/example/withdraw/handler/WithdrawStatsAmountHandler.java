package io.example.withdraw.handler;

import io.example.withdraw.service.WithdrawStatsAmountService;
import io.vertx.core.Future;
import pb.withdraw.Withdraw.FindYearWithdrawCardNumber;
import pb.withdraw.Withdraw.FindYearWithdrawStatus;
import pb.withdraw.stats.WithdrawStatsAmount.*;

public class WithdrawStatsAmountHandler implements pb.withdraw.stats.VertxWithdrawStatsAmountServiceGrpcServer.WithdrawStatsAmountServiceApi {
  private final WithdrawStatsAmountService service;

  public WithdrawStatsAmountHandler(WithdrawStatsAmountService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseWithdrawMonthAmount> findMonthlyWithdraws(FindYearWithdrawStatus req) {
    return service.getMonthlyWithdrawAmounts(req)
        .map(res -> ApiResponseWithdrawMonthAmount.newBuilder()
            .setStatus("success")
            .setMessage("Monthly withdrawals report ready")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawYearAmount> findYearlyWithdraws(FindYearWithdrawStatus req) {
    return service.getYearlyWithdrawAmounts(req)
        .map(res -> ApiResponseWithdrawYearAmount.newBuilder()
            .setStatus("success")
            .setMessage("Yearly withdrawals report ready")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawMonthAmount> findMonthlyWithdrawsByCardNumber(FindYearWithdrawCardNumber req) {
    return service.getMonthlyWithdrawAmountsByCard(req)
        .map(res -> ApiResponseWithdrawMonthAmount.newBuilder()
            .setStatus("success")
            .setMessage("Monthly card withdrawals report ready")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawYearAmount> findYearlyWithdrawsByCardNumber(FindYearWithdrawCardNumber req) {
    return service.getYearlyWithdrawAmountsByCard(req)
        .map(res -> ApiResponseWithdrawYearAmount.newBuilder()
            .setStatus("success")
            .setMessage("Yearly card withdrawals report ready")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList())
            .build());
  }
}
