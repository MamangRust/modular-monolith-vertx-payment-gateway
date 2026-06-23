package io.example.card.handler;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.service.CardStatsBalanceService;
import io.example.common.grpc.GrpcExceptionMapper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.stats.CardStatsBalance.ApiResponseMonthlyBalance;
import pb.card.stats.CardStatsBalance.ApiResponseYearlyBalance;
import pb.card.stats.CardStatsBalance.CardResponseMonthlyBalance;
import pb.card.stats.CardStatsBalance.CardResponseYearlyBalance;
import pb.card.stats.CardStatsBalance.FindYearBalance;
import pb.card.stats.CardStatsBalance.FindYearBalanceCardNumber;

@RequiredArgsConstructor
public class CardStatsBalanceHandler
        implements pb.card.stats.VertxCardStatsBalanceServiceGrpcServer.CardStatsBalanceServiceApi {
    private final CardStatsBalanceService service;

    @Override
    public Future<ApiResponseMonthlyBalance> findMonthlyBalance(FindYearBalance req) {
        return service.getMonthlyBalances(req.getYear())
                .map(list -> {
                    var builder = ApiResponseMonthlyBalance.newBuilder().setStatus("success").setMessage("OK");
                    list.stream()
                            .map(d -> CardResponseMonthlyBalance.newBuilder().setMonth(String.valueOf(d.getMonth()))
                                    .setTotalBalance(d.getBalance()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseYearlyBalance> findYearlyBalance(FindYearBalance req) {
        return service.getYearlyBalances(req.getYear())
                .map(list -> {
                    var builder = ApiResponseYearlyBalance.newBuilder().setStatus("success").setMessage("OK");
                    list.stream()
                            .map(d -> CardResponseYearlyBalance.newBuilder().setYear(String.valueOf(d.getYear()))
                                    .setTotalBalance(d.getBalance()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMonthlyBalance> findMonthlyBalanceByCardNumber(FindYearBalanceCardNumber req) {
        var reqDomain = MonthYearCardNumberCard.builder()
                .year(req.getYear())
                .cardNumber(req.getCardNumber())
                .build();

        return service.getMonthlyBalancesByCardNumber(reqDomain)
                .map(list -> {
                    var builder = ApiResponseMonthlyBalance.newBuilder().setStatus("success").setMessage("OK");
                    list.stream()
                            .map(d -> CardResponseMonthlyBalance.newBuilder().setMonth(String.valueOf(d.getMonth()))
                                    .setTotalBalance(d.getBalance()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseYearlyBalance> findYearlyBalanceByCardNumber(FindYearBalanceCardNumber req) {
        var reqDomain = MonthYearCardNumberCard.builder()
                .year(req.getYear())
                .cardNumber(req.getCardNumber())
                .build();

        return service.getYearlyBalancesByCardNumber(reqDomain)
                .map(list -> {
                    var builder = ApiResponseYearlyBalance.newBuilder().setStatus("success").setMessage("OK");
                    list.stream()
                            .map(d -> CardResponseYearlyBalance.newBuilder().setYear(String.valueOf(d.getYear()))
                                    .setTotalBalance(d.getBalance()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }
}