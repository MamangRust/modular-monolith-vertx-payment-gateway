package io.example.merchant.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.domain.requests.merchant.MonthYearPaymentMethodApiKey;
import io.example.merchant.domain.requests.merchant.MonthYearPaymentMethodMerchant;
import io.example.merchant.service.MerchantStatsMethodByApiKeyService;
import io.example.merchant.service.MerchantStatsMethodByMerchantService;
import io.example.merchant.service.MerchantStatsMethodService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.FindYearMerchant;
import pb.merchant.Merchant.FindYearMerchantByApikey;
import pb.merchant.Merchant.FindYearMerchantById;
import pb.merchant.stats.MerchantStatsMethod.ApiResponseMerchantMonthlyPaymentMethod;
import pb.merchant.stats.MerchantStatsMethod.ApiResponseMerchantYearlyPaymentMethod;
import pb.merchant.stats.MerchantStatsMethod.MerchantResponseMonthlyPaymentMethod;
import pb.merchant.stats.MerchantStatsMethod.MerchantResponseYearlyPaymentMethod;

@RequiredArgsConstructor
public class MerchantStatsMethodHandler
        implements pb.merchant.stats.VertxMerchantStatsMethodServiceGrpcServer.MerchantStatsMethodServiceApi {
    private final MerchantStatsMethodService globalService;
    private final MerchantStatsMethodByApiKeyService apiKeyService;
    private final MerchantStatsMethodByMerchantService merchantService;

    @Override
    public Future<ApiResponseMerchantMonthlyPaymentMethod> findMonthlyPaymentMethodsMerchant(FindYearMerchant req) {
        return globalService.getMonthlyMethodAmounts(req)
                .map(list -> {
                    var builder = ApiResponseMerchantMonthlyPaymentMethod.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseMonthlyPaymentMethod.newBuilder().setMonth(r.getMonth())
                                    .setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMerchantYearlyPaymentMethod> findYearlyPaymentMethodMerchant(FindYearMerchant req) {
        return globalService.getYearlyMethodAmounts(req)
                .map(list -> {
                    var builder = ApiResponseMerchantYearlyPaymentMethod.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseYearlyPaymentMethod.newBuilder().setYear(r.getYear())
                                    .setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMerchantMonthlyPaymentMethod> findMonthlyPaymentMethodByMerchants(
            FindYearMerchantById req) {
        var reqDomain = MonthYearPaymentMethodMerchant.builder()
                .merchantId(req.getMerchantId())
                .year(req.getYear())
                .build();

        return merchantService.getMonthlyMethodAmounts(reqDomain)
                .map(list -> {
                    var builder = ApiResponseMerchantMonthlyPaymentMethod.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseMonthlyPaymentMethod.newBuilder().setMonth(r.getMonth())
                                    .setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMerchantYearlyPaymentMethod> findYearlyPaymentMethodByMerchants(FindYearMerchantById req) {
        var reqDomain = MonthYearPaymentMethodMerchant.builder()
                .merchantId(req.getMerchantId())
                .year(req.getYear())
                .build();

        return merchantService.getYearlyMethodAmounts(reqDomain)
                .map(list -> {
                    var builder = ApiResponseMerchantYearlyPaymentMethod.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseYearlyPaymentMethod.newBuilder().setYear(r.getYear())
                                    .setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMerchantMonthlyPaymentMethod> findMonthlyPaymentMethodByApikey(
            FindYearMerchantByApikey req) {
        var reqDomain = MonthYearPaymentMethodApiKey.builder()
                .apikey(req.getApiKey())
                .year(req.getYear())
                .build();

        return apiKeyService.getMonthlyMethodAmounts(reqDomain)
                .map(list -> {
                    var builder = ApiResponseMerchantMonthlyPaymentMethod.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseMonthlyPaymentMethod.newBuilder().setMonth(r.getMonth())
                                    .setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMerchantYearlyPaymentMethod> findYearlyPaymentMethodByApikey(
            FindYearMerchantByApikey req) {
        var reqDomain = MonthYearPaymentMethodApiKey.builder()
                .apikey(req.getApiKey())
                .year(req.getYear())
                .build();

        return apiKeyService.getYearlyMethodAmounts(reqDomain)
                .map(list -> {
                    var builder = ApiResponseMerchantYearlyPaymentMethod.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseYearlyPaymentMethod.newBuilder().setYear(r.getYear())
                                    .setPaymentMethod(r.getPaymentMethod()).setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }
}