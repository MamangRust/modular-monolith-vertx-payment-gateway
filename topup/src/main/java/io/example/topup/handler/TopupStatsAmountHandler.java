package io.example.topup.handler;

import io.example.topup.service.TopupStatsAmountService;
import io.vertx.core.Future;
import pb.topup.Topup.FindYearTopupCardNumber;
import pb.topup.Topup.FindYearTopupStatus;
import pb.topup.stats.TopupStatsAmount.ApiResponseTopupMonthAmount;
import pb.topup.stats.TopupStatsAmount.ApiResponseTopupYearAmount;

public class TopupStatsAmountHandler
    implements pb.topup.stats.VertxTopupStatsAmountServiceGrpcServer.TopupStatsAmountServiceApi {
  private final TopupStatsAmountService service;

  public TopupStatsAmountHandler(TopupStatsAmountService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseTopupMonthAmount> findMonthlyTopupAmounts(FindYearTopupStatus req) {
    return service.getMonthlyTopupAmounts(req)
        .map(res -> ApiResponseTopupMonthAmount.newBuilder()
            .setStatus("success")
            .setMessage("Monthly topups volume computed")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupYearAmount> findYearlyTopupAmounts(FindYearTopupStatus req) {
    return service.getYearlyTopupAmounts(req)
        .map(res -> ApiResponseTopupYearAmount.newBuilder()
            .setStatus("success")
            .setMessage("Yearly topups volume computed")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupMonthAmount> findMonthlyTopupAmountsByCardNumber(FindYearTopupCardNumber req) {
    return service.getMonthlyTopupAmountsByCard(req)
        .map(res -> ApiResponseTopupMonthAmount.newBuilder()
            .setStatus("success")
            .setMessage("Monthly card topups volume computed")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupYearAmount> findYearlyTopupAmountsByCardNumber(FindYearTopupCardNumber req) {
    return service.getYearlyTopupAmountsByCard(req)
        .map(res -> ApiResponseTopupYearAmount.newBuilder()
            .setStatus("success")
            .setMessage("Yearly card topups volume computed")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList())
            .build());
  }
}
