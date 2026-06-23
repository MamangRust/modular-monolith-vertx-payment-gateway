package io.example.topup.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.topup.domain.requests.topup.YearTopupCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupRequest;
import io.example.topup.service.TopupStatsAmountService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.topup.Topup.FindYearTopupCardNumber;
import pb.topup.Topup.FindYearTopupStatus;
import pb.topup.stats.TopupStatsAmount.ApiResponseTopupMonthAmount;
import pb.topup.stats.TopupStatsAmount.ApiResponseTopupYearAmount;

@RequiredArgsConstructor
public class TopupStatsAmountHandler
    implements pb.topup.stats.VertxTopupStatsAmountServiceGrpcServer.TopupStatsAmountServiceApi {
  private final TopupStatsAmountService service;

  @Override
  public Future<ApiResponseTopupMonthAmount> findMonthlyTopupAmounts(FindYearTopupStatus req) {
    var domainReq = YearTopupRequest.builder().year(req.getYear()).build();
    return service.getMonthlyTopupAmounts(domainReq)
        .map(res -> ApiResponseTopupMonthAmount.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopupYearAmount> findYearlyTopupAmounts(FindYearTopupStatus req) {
    var domainReq = YearTopupRequest.builder().year(req.getYear()).build();
    return service.getYearlyTopupAmounts(domainReq)
        .map(res -> ApiResponseTopupYearAmount.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopupMonthAmount> findMonthlyTopupAmountsByCardNumber(FindYearTopupCardNumber req) {
    var domainReq = YearTopupCardNumberRequest.builder().cardNumber(req.getCardNumber()).year(req.getYear()).build();
    return service.getMonthlyTopupAmountsByCard(domainReq)
        .map(res -> ApiResponseTopupMonthAmount.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(res.stream().map(ProtoConverter::toMonthAmount).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopupYearAmount> findYearlyTopupAmountsByCardNumber(FindYearTopupCardNumber req) {
    var domainReq = YearTopupCardNumberRequest.builder().cardNumber(req.getCardNumber()).year(req.getYear()).build();
    return service.getYearlyTopupAmountsByCard(domainReq)
        .map(res -> ApiResponseTopupYearAmount.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(res.stream().map(ProtoConverter::toYearlyAmount).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}