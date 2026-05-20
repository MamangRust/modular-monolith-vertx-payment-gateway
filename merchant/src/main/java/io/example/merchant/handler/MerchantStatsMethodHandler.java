package io.example.merchant.handler;

import io.example.merchant.service.MerchantStatsMethodService;
import io.example.merchant.service.MerchantStatsMethodByApiKeyService;
import io.example.merchant.service.MerchantStatsMethodByMerchantService;
import io.vertx.core.Future;
import pb.merchant.stats.MerchantStatsMethod.*;
import pb.merchant.Merchant.*;

public class MerchantStatsMethodHandler implements pb.merchant.stats.VertxMerchantStatsMethodServiceGrpcServer.MerchantStatsMethodServiceApi {
  private final MerchantStatsMethodService globalService;
  private final MerchantStatsMethodByApiKeyService apiKeyService;
  private final MerchantStatsMethodByMerchantService merchantService;

  public MerchantStatsMethodHandler(
      MerchantStatsMethodService globalService,
      MerchantStatsMethodByApiKeyService apiKeyService,
      MerchantStatsMethodByMerchantService merchantService) {
    this.globalService = globalService;
    this.apiKeyService = apiKeyService;
    this.merchantService = merchantService;
  }

  @Override
  public Future<ApiResponseMerchantMonthlyPaymentMethod> findMonthlyPaymentMethodsMerchant(FindYearMerchant req) {
    return globalService.getMonthlyMethodAmounts(req)
        .map(list -> ApiResponseMerchantMonthlyPaymentMethod.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseMonthlyPaymentMethod.newBuilder().setMonth(r.getMonth()).setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantYearlyPaymentMethod> findYearlyPaymentMethodMerchant(FindYearMerchant req) {
    return globalService.getYearlyMethodAmounts(req)
        .map(list -> ApiResponseMerchantYearlyPaymentMethod.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseYearlyPaymentMethod.newBuilder().setYear(r.getYear()).setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantMonthlyPaymentMethod> findMonthlyPaymentMethodByMerchants(FindYearMerchantById req) {
    return merchantService.getMonthlyMethodAmounts(req)
        .map(list -> ApiResponseMerchantMonthlyPaymentMethod.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseMonthlyPaymentMethod.newBuilder().setMonth(r.getMonth()).setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantYearlyPaymentMethod> findYearlyPaymentMethodByMerchants(FindYearMerchantById req) {
    return merchantService.getYearlyMethodAmounts(req)
        .map(list -> ApiResponseMerchantYearlyPaymentMethod.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseYearlyPaymentMethod.newBuilder().setYear(r.getYear()).setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantMonthlyPaymentMethod> findMonthlyPaymentMethodByApikey(FindYearMerchantByApikey req) {
    return apiKeyService.getMonthlyMethodAmounts(req)
        .map(list -> ApiResponseMerchantMonthlyPaymentMethod.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseMonthlyPaymentMethod.newBuilder().setMonth(r.getMonth()).setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantYearlyPaymentMethod> findYearlyPaymentMethodByApikey(FindYearMerchantByApikey req) {
    return apiKeyService.getYearlyMethodAmounts(req)
        .map(list -> ApiResponseMerchantYearlyPaymentMethod.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseYearlyPaymentMethod.newBuilder().setYear(r.getYear()).setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }
}
