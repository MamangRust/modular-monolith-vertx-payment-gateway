package io.example.topup.handler;

import io.example.topup.domain.requests.topup.MonthTopupStatusCardNumberRequest;
import io.example.topup.domain.requests.topup.MonthTopupStatusRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusCardNumberRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusRequest;
import io.example.topup.service.TopupStatsStatusService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.topup.Topup.FindMonthlyTopupStatus;
import pb.topup.Topup.FindMonthlyTopupStatusCardNumber;
import pb.topup.Topup.FindYearTopupStatus;
import pb.topup.Topup.FindYearTopupStatusCardNumber;
import pb.topup.stats.TopupStatsStatus.ApiResponseTopupMonthStatusFailed;
import pb.topup.stats.TopupStatsStatus.ApiResponseTopupMonthStatusSuccess;
import pb.topup.stats.TopupStatsStatus.ApiResponseTopupYearStatusFailed;
import pb.topup.stats.TopupStatsStatus.ApiResponseTopupYearStatusSuccess;

@RequiredArgsConstructor
public class TopupStatsStatusHandler
        implements pb.topup.stats.VertxTopupStatsStatusServiceGrpcServer.TopupStatsStatusServiceApi {
    private final TopupStatsStatusService service;

    @Override
    public Future<ApiResponseTopupMonthStatusSuccess> findMonthlyTopupStatusSuccess(FindMonthlyTopupStatus req) {
        var domainReq = MonthTopupStatusRequest.builder()
                .year(req.getYear())
                .month(req.getMonth())
                .status("success")
                .build();

        return service.getMonthlyTopupStatus(domainReq)
                .map(res -> ApiResponseTopupMonthStatusSuccess.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly success topup counts aggregated")
                        .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTopupYearStatusSuccess> findYearlyTopupStatusSuccess(FindYearTopupStatus req) {
        var domainReq = YearTopupStatusRequest.builder()
                .year(req.getYear())
                .status("success")
                .build();

        return service.getYearlyTopupStatus(domainReq)
                .map(res -> ApiResponseTopupYearStatusSuccess.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly success topup counts aggregated")
                        .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTopupMonthStatusFailed> findMonthlyTopupStatusFailed(FindMonthlyTopupStatus req) {
        var domainReq = MonthTopupStatusRequest.builder()
                .year(req.getYear())
                .month(req.getMonth())
                .status("failed")
                .build();

        return service.getMonthlyTopupStatus(domainReq)
                .map(res -> ApiResponseTopupMonthStatusFailed.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly failed topup counts aggregated")
                        .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTopupYearStatusFailed> findYearlyTopupStatusFailed(FindYearTopupStatus req) {
        var domainReq = YearTopupStatusRequest.builder()
                .year(req.getYear())
                .status("failed")
                .build();

        return service.getYearlyTopupStatus(domainReq)
                .map(res -> ApiResponseTopupYearStatusFailed.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly failed topup counts aggregated")
                        .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTopupMonthStatusSuccess> findMonthlyTopupStatusSuccessByCardNumber(
            FindMonthlyTopupStatusCardNumber req) {
        var domainReq = MonthTopupStatusCardNumberRequest.builder()
                .year(req.getYear())
                .month(req.getMonth())
                .cardNumber(req.getCardNumber())
                .status("success")
                .build();

        return service.getMonthlyTopupStatusByCard(domainReq)
                .map(res -> ApiResponseTopupMonthStatusSuccess.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly card success topups computed")
                        .addAllData(res.stream().map(ProtoConverter::toMonthSuccess).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTopupYearStatusSuccess> findYearlyTopupStatusSuccessByCardNumber(
            FindYearTopupStatusCardNumber req) {
        var domainReq = YearTopupStatusCardNumberRequest.builder()
                .year(req.getYear())
                .cardNumber(req.getCardNumber())
                .status("success")
                .build();

        return service.getYearlyTopupStatusByCard(domainReq)
                .map(res -> ApiResponseTopupYearStatusSuccess.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly card success topups computed")
                        .addAllData(res.stream().map(ProtoConverter::toYearlySuccess).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTopupMonthStatusFailed> findMonthlyTopupStatusFailedByCardNumber(
            FindMonthlyTopupStatusCardNumber req) {
        var domainReq = MonthTopupStatusCardNumberRequest.builder()
                .year(req.getYear())
                .month(req.getMonth())
                .cardNumber(req.getCardNumber())
                .status("failed")
                .build();

        return service.getMonthlyTopupStatusByCard(domainReq)
                .map(res -> ApiResponseTopupMonthStatusFailed.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly card failed topups computed")
                        .addAllData(res.stream().map(ProtoConverter::toMonthFailed).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTopupYearStatusFailed> findYearlyTopupStatusFailedByCardNumber(
            FindYearTopupStatusCardNumber req) {
        var domainReq = YearTopupStatusCardNumberRequest.builder()
                .year(req.getYear())
                .cardNumber(req.getCardNumber())
                .status("failed")
                .build();

        return service.getYearlyTopupStatusByCard(domainReq)
                .map(res -> ApiResponseTopupYearStatusFailed.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly card failed topups computed")
                        .addAllData(res.stream().map(ProtoConverter::toYearlyFailed).toList())
                        .build());
    }
}
