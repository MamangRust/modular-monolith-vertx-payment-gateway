package io.example.merchant.handler;

import io.example.merchant.service.MerchantStatsTotalAmountService;
import io.example.merchant.service.MerchantStatsTotalAmountByApiKeyService;
import io.example.merchant.service.MerchantStatsTotalAmountByMerchantService;
import io.vertx.core.Future;
import pb.merchant.stats.MerchantStatsTotalamount.*;
import pb.merchant.Merchant.*;

public class MerchantStatsTotalAmountHandler implements pb.merchant.stats.VertxMerchantStatsTotalAmountServiceGrpcServer.MerchantStatsTotalAmountServiceApi {
  private final MerchantStatsTotalAmountService globalService;
  private final MerchantStatsTotalAmountByApiKeyService apiKeyService;
  private final MerchantStatsTotalAmountByMerchantService merchantService;

  public MerchantStatsTotalAmountHandler(
      MerchantStatsTotalAmountService globalService,
      MerchantStatsTotalAmountByApiKeyService apiKeyService,
      MerchantStatsTotalAmountByMerchantService merchantService) {
    this.globalService = globalService;
    this.apiKeyService = apiKeyService;
    this.merchantService = merchantService;
  }

  @Override
  public Future<ApiResponseMerchantMonthlyTotalAmount> findMonthlyTotalAmountMerchant(FindYearMerchant req) {
    return globalService.getMonthlyTotalAmounts(req)
        .map(list -> ApiResponseMerchantMonthlyTotalAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseMonthlyTotalAmount.newBuilder().setMonth(r.getMonth()).setYear(String.valueOf(req.getYear())).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantYearlyTotalAmount> findYearlyTotalAmountMerchant(FindYearMerchant req) {
    return globalService.getYearlyTotalAmounts(req)
        .map(list -> ApiResponseMerchantYearlyTotalAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseYearlyTotalAmount.newBuilder().setYear(r.getYear()).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantMonthlyTotalAmount> findMonthlyTotalAmountByMerchants(FindYearMerchantById req) {
    return merchantService.getMonthlyTotalAmounts(req)
        .map(list -> ApiResponseMerchantMonthlyTotalAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseMonthlyTotalAmount.newBuilder().setMonth(r.getMonth()).setYear(String.valueOf(req.getYear())).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantYearlyTotalAmount> findYearlyTotalAmountByMerchants(FindYearMerchantById req) {
    return merchantService.getYearlyTotalAmounts(req)
        .map(list -> ApiResponseMerchantYearlyTotalAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseYearlyTotalAmount.newBuilder().setYear(r.getYear()).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantMonthlyTotalAmount> findMonthlyTotalAmountByApikey(FindYearMerchantByApikey req) {
    return apiKeyService.getMonthlyTotalAmounts(req)
        .map(list -> ApiResponseMerchantMonthlyTotalAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseMonthlyTotalAmount.newBuilder().setMonth(r.getMonth()).setYear(String.valueOf(req.getYear())).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantYearlyTotalAmount> findYearlyTotalAmountByApikey(FindYearMerchantByApikey req) {
    return apiKeyService.getYearlyTotalAmounts(req)
        .map(list -> ApiResponseMerchantYearlyTotalAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream().map(r -> MerchantResponseYearlyTotalAmount.newBuilder().setYear(r.getYear()).setTotalAmount(r.getAmount()).build()).toList())
            .build());
  }
}
