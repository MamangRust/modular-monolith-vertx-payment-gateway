package io.example.merchant.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.domain.requests.merchant.MonthYearAmountApiKey;
import io.example.merchant.domain.requests.merchant.MonthYearAmountMerchant;
import io.example.merchant.service.MerchantStatsAmountByApiKeyService;
import io.example.merchant.service.MerchantStatsAmountByMerchantService;
import io.example.merchant.service.MerchantStatsAmountService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.FindYearMerchant;
import pb.merchant.Merchant.FindYearMerchantByApikey;
import pb.merchant.Merchant.FindYearMerchantById;
import pb.merchant.stats.MerchantStatsAmount.ApiResponseMerchantMonthlyAmount;
import pb.merchant.stats.MerchantStatsAmount.ApiResponseMerchantYearlyAmount;
import pb.merchant.stats.MerchantStatsAmount.MerchantResponseMonthlyAmount;
import pb.merchant.stats.MerchantStatsAmount.MerchantResponseYearlyAmount;

@RequiredArgsConstructor
public class MerchantStatsAmountHandler
                implements pb.merchant.stats.VertxMerchantStatsAmountServiceGrpcServer.MerchantStatsAmountServiceApi {
        private final MerchantStatsAmountService globalService;
        private final MerchantStatsAmountByApiKeyService apiKeyService;
        private final MerchantStatsAmountByMerchantService merchantService;

        @Override
        public Future<ApiResponseMerchantMonthlyAmount> findMonthlyAmountMerchant(FindYearMerchant req) {
                return globalService.getMonthlyAmounts(req)
                                .map(list -> {
                                        var builder = ApiResponseMerchantMonthlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(r -> MerchantResponseMonthlyAmount.newBuilder()
                                                        .setMonth(r.getMonth()).setTotalAmount(r.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantYearlyAmount> findYearlyAmountMerchant(FindYearMerchant req) {
                return globalService.getYearlyAmounts(req)
                                .map(list -> {
                                        var builder = ApiResponseMerchantYearlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(r -> MerchantResponseYearlyAmount.newBuilder()
                                                        .setYear(r.getYear()).setTotalAmount(r.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantMonthlyAmount> findMonthlyAmountByMerchants(FindYearMerchantById req) {
                var reqDomain = MonthYearAmountMerchant.builder()
                                .merchantId(req.getMerchantId())
                                .year(req.getYear())
                                .build();

                return merchantService.getMonthlyAmounts(reqDomain)
                                .map(list -> {
                                        var builder = ApiResponseMerchantMonthlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(r -> MerchantResponseMonthlyAmount.newBuilder()
                                                        .setMonth(r.getMonth()).setTotalAmount(r.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantYearlyAmount> findYearlyAmountByMerchants(FindYearMerchantById req) {
                var reqDomain = MonthYearAmountMerchant.builder()
                                .merchantId(req.getMerchantId())
                                .year(req.getYear())
                                .build();

                return merchantService.getYearlyAmounts(reqDomain)
                                .map(list -> {
                                        var builder = ApiResponseMerchantYearlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(r -> MerchantResponseYearlyAmount.newBuilder()
                                                        .setYear(r.getYear()).setTotalAmount(r.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantMonthlyAmount> findMonthlyAmountByApikey(FindYearMerchantByApikey req) {
                var reqDomain = MonthYearAmountApiKey.builder()
                                .apikey(req.getApiKey())
                                .year(req.getYear())
                                .build();

                return apiKeyService.getMonthlyAmounts(reqDomain)
                                .map(list -> {
                                        var builder = ApiResponseMerchantMonthlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(r -> MerchantResponseMonthlyAmount.newBuilder()
                                                        .setMonth(r.getMonth()).setTotalAmount(r.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantYearlyAmount> findYearlyAmountByApikey(FindYearMerchantByApikey req) {
                var reqDomain = MonthYearAmountApiKey.builder()
                                .apikey(req.getApiKey())
                                .year(req.getYear())
                                .build();

                return apiKeyService.getYearlyAmounts(reqDomain)
                                .map(list -> {
                                        var builder = ApiResponseMerchantYearlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(r -> MerchantResponseYearlyAmount.newBuilder()
                                                        .setYear(r.getYear()).setTotalAmount(r.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}