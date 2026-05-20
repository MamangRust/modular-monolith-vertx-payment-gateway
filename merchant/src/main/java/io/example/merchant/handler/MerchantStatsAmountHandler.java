package io.example.merchant.handler;

import io.example.merchant.service.MerchantStatsAmountByApiKeyService;
import io.example.merchant.service.MerchantStatsAmountByMerchantService;
import io.example.merchant.service.MerchantStatsAmountService;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindYearMerchant;
import pb.merchant.Merchant.FindYearMerchantByApikey;
import pb.merchant.Merchant.FindYearMerchantById;
import pb.merchant.stats.MerchantStatsAmount.ApiResponseMerchantMonthlyAmount;
import pb.merchant.stats.MerchantStatsAmount.ApiResponseMerchantYearlyAmount;
import pb.merchant.stats.MerchantStatsAmount.MerchantResponseMonthlyAmount;
import pb.merchant.stats.MerchantStatsAmount.MerchantResponseYearlyAmount;

public class MerchantStatsAmountHandler
    implements pb.merchant.stats.VertxMerchantStatsAmountServiceGrpcServer.MerchantStatsAmountServiceApi {
  private final MerchantStatsAmountService globalService;
  private final MerchantStatsAmountByApiKeyService apiKeyService;
  private final MerchantStatsAmountByMerchantService merchantService;

  public MerchantStatsAmountHandler(
      MerchantStatsAmountService globalService,
      MerchantStatsAmountByApiKeyService apiKeyService,
      MerchantStatsAmountByMerchantService merchantService) {
    this.globalService = globalService;
    this.apiKeyService = apiKeyService;
    this.merchantService = merchantService;
  }

  @Override
  public Future<ApiResponseMerchantMonthlyAmount> findMonthlyAmountMerchant(FindYearMerchant req) {
    return globalService.getMonthlyAmounts(req)
        .map(list -> ApiResponseMerchantMonthlyAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream()
                .map(r -> MerchantResponseMonthlyAmount.newBuilder().setMonth(r.getMonth())
                    .setTotalAmount(r.getAmount()).build())
                .toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantYearlyAmount> findYearlyAmountMerchant(FindYearMerchant req) {
    return globalService.getYearlyAmounts(req)
        .map(list -> ApiResponseMerchantYearlyAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream()
                .map(r -> MerchantResponseYearlyAmount.newBuilder().setYear(r.getYear()).setTotalAmount(r.getAmount())
                    .build())
                .toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantMonthlyAmount> findMonthlyAmountByMerchants(FindYearMerchantById req) {
    return merchantService.getMonthlyAmounts(req)
        .map(list -> ApiResponseMerchantMonthlyAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream()
                .map(r -> MerchantResponseMonthlyAmount.newBuilder().setMonth(r.getMonth())
                    .setTotalAmount(r.getAmount()).build())
                .toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantYearlyAmount> findYearlyAmountByMerchants(FindYearMerchantById req) {
    return merchantService.getYearlyAmounts(req)
        .map(list -> ApiResponseMerchantYearlyAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream()
                .map(r -> MerchantResponseYearlyAmount.newBuilder().setYear(r.getYear()).setTotalAmount(r.getAmount())
                    .build())
                .toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantMonthlyAmount> findMonthlyAmountByApikey(FindYearMerchantByApikey req) {
    return apiKeyService.getMonthlyAmounts(req)
        .map(list -> ApiResponseMerchantMonthlyAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream()
                .map(r -> MerchantResponseMonthlyAmount.newBuilder().setMonth(r.getMonth())
                    .setTotalAmount(r.getAmount()).build())
                .toList())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantYearlyAmount> findYearlyAmountByApikey(FindYearMerchantByApikey req) {
    return apiKeyService.getYearlyAmounts(req)
        .map(list -> ApiResponseMerchantYearlyAmount.newBuilder()
            .setStatus("success").setMessage("Loaded")
            .addAllData(list.stream()
                .map(r -> MerchantResponseYearlyAmount.newBuilder().setYear(r.getYear()).setTotalAmount(r.getAmount())
                    .build())
                .toList())
            .build());
  }
}
