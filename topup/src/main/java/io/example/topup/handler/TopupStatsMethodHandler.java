package io.example.topup.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.topup.domain.requests.topup.YearTopupCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupRequest;
import io.example.topup.model.TopupStats;
import io.example.topup.service.TopupStatsMethodService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.topup.Topup.FindYearTopupCardNumber;
import pb.topup.Topup.FindYearTopupStatus;
import pb.topup.stats.TopupStatsMethod.ApiResponseTopupMonthMethod;
import pb.topup.stats.TopupStatsMethod.ApiResponseTopupYearMethod;

@RequiredArgsConstructor
public class TopupStatsMethodHandler
    implements pb.topup.stats.VertxTopupStatsMethodServiceGrpcServer.TopupStatsMethodServiceApi {
  private final TopupStatsMethodService service;

  @Override
  public Future<ApiResponseTopupMonthMethod> findMonthlyTopupMethods(FindYearTopupStatus req) {
    var domainReq = YearTopupRequest.builder().year(req.getYear()).build();
    return service.getMonthlyTopupMethods(domainReq)
        .map(res -> ApiResponseTopupMonthMethod.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(res.stream().map(ProtoConverter::toMonthMethod).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopupYearMethod> findYearlyTopupMethods(FindYearTopupStatus req) {
    var domainReq = YearTopupRequest.builder().year(req.getYear()).build();

    return service.getYearlyTopupMethods(domainReq)
        .map(res -> {
          ApiResponseTopupYearMethod.Builder builder = ApiResponseTopupYearMethod.newBuilder()
              .setStatus("success")
              .setMessage("OK");

          for (TopupStats.YearMethod month : res) {
            builder.addData(ProtoConverter.toYearlyMethod(month));
          }
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopupMonthMethod> findMonthlyTopupMethodsByCardNumber(FindYearTopupCardNumber req) {
    var domainReq = YearTopupCardNumberRequest.builder().cardNumber(req.getCardNumber()).year(req.getYear()).build();
    return service.getMonthlyTopupMethodsByCard(domainReq)
        .map(res -> ApiResponseTopupMonthMethod.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(res.stream().map(ProtoConverter::toMonthMethod).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopupYearMethod> findYearlyTopupMethodsByCardNumber(FindYearTopupCardNumber req) {
    var domainReq = YearTopupCardNumberRequest.builder().cardNumber(req.getCardNumber()).year(req.getYear()).build();

    return service.getYearlyTopupMethodsByCard(domainReq)
        .map(res -> {
          ApiResponseTopupYearMethod.Builder builder = ApiResponseTopupYearMethod.newBuilder()
              .setStatus("success")
              .setMessage("OK");

          for (TopupStats.YearMethod month : res) {
            builder.addData(ProtoConverter.toYearlyMethod(month));
          }
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}