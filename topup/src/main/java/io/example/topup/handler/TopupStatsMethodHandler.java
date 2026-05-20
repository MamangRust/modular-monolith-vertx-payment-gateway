package io.example.topup.handler;

import io.example.topup.service.TopupStatsMethodService;
import io.vertx.core.Future;
import pb.topup.Topup.*;
import pb.topup.stats.TopupStatsMethod.*;

public class TopupStatsMethodHandler implements pb.topup.stats.VertxTopupStatsMethodServiceGrpcServer.TopupStatsMethodServiceApi {
  private final TopupStatsMethodService service;

  public TopupStatsMethodHandler(TopupStatsMethodService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseTopupMonthMethod> findMonthlyTopupMethods(FindYearTopupStatus req) {
    return service.getMonthlyTopupMethods(req)
        .map(res -> ApiResponseTopupMonthMethod.newBuilder()
            .setStatus("success")
            .setMessage("Monthly topup methods segmented")
            .addAllData(res.stream().map(ProtoConverter::toMonthMethod).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupYearMethod> findYearlyTopupMethods(FindYearTopupStatus req) {
    return service.getYearlyTopupMethods(req)
        .map(res -> ApiResponseTopupYearMethod.newBuilder()
            .setStatus("success")
            .setMessage("Yearly topup methods segmented")
            .addAllData(res.stream().map(ProtoConverter::toYearlyMethod).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupMonthMethod> findMonthlyTopupMethodsByCardNumber(FindYearTopupCardNumber req) {
    return service.getMonthlyTopupMethodsByCard(req)
        .map(res -> ApiResponseTopupMonthMethod.newBuilder()
            .setStatus("success")
            .setMessage("Monthly card topup methods segmented")
            .addAllData(res.stream().map(ProtoConverter::toMonthMethod).toList())
            .build());
  }

  @Override
  public Future<ApiResponseTopupYearMethod> findYearlyTopupMethodsByCardNumber(FindYearTopupCardNumber req) {
    return service.getYearlyTopupMethodsByCard(req)
        .map(res -> ApiResponseTopupYearMethod.newBuilder()
            .setStatus("success")
            .setMessage("Yearly card topup methods segmented")
            .addAllData(res.stream().map(ProtoConverter::toYearlyMethod).toList())
            .build());
  }
}
