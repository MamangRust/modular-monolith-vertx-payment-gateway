package io.example.merchant.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.domain.requests.merchant.MonthYearTotalAmountApiKey;
import io.example.merchant.domain.requests.merchant.MonthYearTotalAmountMerchant;
import io.example.merchant.service.MerchantStatsTotalAmountByApiKeyService;
import io.example.merchant.service.MerchantStatsTotalAmountByMerchantService;
import io.example.merchant.service.MerchantStatsTotalAmountService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.FindYearMerchant;
import pb.merchant.Merchant.FindYearMerchantByApikey;
import pb.merchant.Merchant.FindYearMerchantById;
import pb.merchant.stats.MerchantStatsTotalamount.ApiResponseMerchantMonthlyTotalAmount;
import pb.merchant.stats.MerchantStatsTotalamount.ApiResponseMerchantYearlyTotalAmount;
import pb.merchant.stats.MerchantStatsTotalamount.MerchantResponseMonthlyTotalAmount;
import pb.merchant.stats.MerchantStatsTotalamount.MerchantResponseYearlyTotalAmount;

@RequiredArgsConstructor
public class MerchantStatsTotalAmountHandler
        implements pb.merchant.stats.VertxMerchantStatsTotalAmountServiceGrpcServer.MerchantStatsTotalAmountServiceApi {
    private final MerchantStatsTotalAmountService globalService;
    private final MerchantStatsTotalAmountByApiKeyService apiKeyService;
    private final MerchantStatsTotalAmountByMerchantService merchantService;

    @Override
    public Future<ApiResponseMerchantMonthlyTotalAmount> findMonthlyTotalAmountMerchant(FindYearMerchant req) {
        return globalService.getMonthlyTotalAmounts(req)
                .map(list -> {
                    var builder = ApiResponseMerchantMonthlyTotalAmount.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseMonthlyTotalAmount.newBuilder().setMonth(r.getMonth())
                                    .setYear(String.valueOf(req.getYear())).setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMerchantYearlyTotalAmount> findYearlyTotalAmountMerchant(FindYearMerchant req) {
        return globalService.getYearlyTotalAmounts(req)
                .map(list -> {
                    var builder = ApiResponseMerchantYearlyTotalAmount.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseYearlyTotalAmount.newBuilder().setYear(r.getYear())
                                    .setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMerchantMonthlyTotalAmount> findMonthlyTotalAmountByMerchants(FindYearMerchantById req) {
        var reqDomain = MonthYearTotalAmountMerchant.builder()
                .merchantId(req.getMerchantId())
                .year(req.getYear())
                .build();

        return merchantService.getMonthlyTotalAmounts(reqDomain)
                .map(list -> {
                    var builder = ApiResponseMerchantMonthlyTotalAmount.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseMonthlyTotalAmount.newBuilder().setMonth(r.getMonth())
                                    .setYear(String.valueOf(req.getYear())).setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMerchantYearlyTotalAmount> findYearlyTotalAmountByMerchants(FindYearMerchantById req) {
        var reqDomain = MonthYearTotalAmountMerchant.builder()
                .merchantId(req.getMerchantId())
                .year(req.getYear())
                .build();

        return merchantService.getYearlyTotalAmounts(reqDomain)
                .map(list -> {
                    var builder = ApiResponseMerchantYearlyTotalAmount.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseYearlyTotalAmount.newBuilder().setYear(r.getYear())
                                    .setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMerchantMonthlyTotalAmount> findMonthlyTotalAmountByApikey(FindYearMerchantByApikey req) {
        var reqDomain = MonthYearTotalAmountApiKey.builder()
                .apikey(req.getApiKey())
                .year(req.getYear())
                .build();

        return apiKeyService.getMonthlyTotalAmounts(reqDomain)
                .map(list -> {
                    var builder = ApiResponseMerchantMonthlyTotalAmount.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseMonthlyTotalAmount.newBuilder().setMonth(r.getMonth())
                                    .setYear(String.valueOf(req.getYear())).setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMerchantYearlyTotalAmount> findYearlyTotalAmountByApikey(FindYearMerchantByApikey req) {
        var reqDomain = MonthYearTotalAmountApiKey.builder()
                .apikey(req.getApiKey())
                .year(req.getYear())
                .build();

        return apiKeyService.getYearlyTotalAmounts(reqDomain)
                .map(list -> {
                    var builder = ApiResponseMerchantYearlyTotalAmount.newBuilder().setStatus("success")
                            .setMessage("OK");
                    list.stream()
                            .map(r -> MerchantResponseYearlyTotalAmount.newBuilder().setYear(r.getYear())
                                    .setTotalAmount(r.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }
}