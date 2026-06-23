package io.example.card.handler;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.service.CardStatsWithdrawService;
import io.example.common.grpc.GrpcExceptionMapper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.Card.ApiResponseMonthlyAmount;
import pb.card.Card.ApiResponseYearlyAmount;
import pb.card.Card.CardResponseMonthlyAmount;
import pb.card.Card.CardResponseYearlyAmount;
import pb.card.Card.FindYearAmount;
import pb.card.Card.FindYearAmountCardNumber;

@RequiredArgsConstructor
public class CardStatsWithdrawHandler
        implements pb.card.stats.VertxCardStatsWithdrawServiceGrpcServer.CardStatsWithdrawServiceApi {
    private final CardStatsWithdrawService service;

    @Override
    public Future<ApiResponseMonthlyAmount> findMonthlyWithdrawAmount(FindYearAmount req) {
        return service.getMonthlyWithdrawAmount(req.getYear())
                .map(list -> {
                    var builder = ApiResponseMonthlyAmount.newBuilder().setStatus("success").setMessage("OK");
                    list.stream()
                            .map(d -> CardResponseMonthlyAmount.newBuilder().setMonth(String.valueOf(d.getMonth()))
                                    .setTotalAmount(d.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseYearlyAmount> findYearlyWithdrawAmount(FindYearAmount req) {
        return service.getYearlyWithdrawAmount(req.getYear())
                .map(list -> {
                    var builder = ApiResponseYearlyAmount.newBuilder().setStatus("success").setMessage("OK");
                    list.stream()
                            .map(d -> CardResponseYearlyAmount.newBuilder().setYear(String.valueOf(d.getYear()))
                                    .setTotalAmount(d.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseMonthlyAmount> findMonthlyWithdrawAmountByCardNumber(FindYearAmountCardNumber req) {
        var reqDomain = MonthYearCardNumberCard.builder()
                .year(req.getYear())
                .cardNumber(req.getCardNumber())
                .build();

        return service.getMonthlyWithdrawAmountByCardNumber(reqDomain)
                .map(list -> {
                    var builder = ApiResponseMonthlyAmount.newBuilder().setStatus("success").setMessage("OK");
                    list.stream()
                            .map(d -> CardResponseMonthlyAmount.newBuilder().setMonth(String.valueOf(d.getMonth()))
                                    .setTotalAmount(d.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseYearlyAmount> findYearlyWithdrawAmountByCardNumber(FindYearAmountCardNumber req) {
        var reqDomain = MonthYearCardNumberCard.builder()
                .year(req.getYear())
                .cardNumber(req.getCardNumber())
                .build();

        return service.getYearlyWithdrawAmountByCardNumber(reqDomain)
                .map(list -> {
                    var builder = ApiResponseYearlyAmount.newBuilder().setStatus("success").setMessage("OK");
                    list.stream()
                            .map(d -> CardResponseYearlyAmount.newBuilder().setYear(String.valueOf(d.getYear()))
                                    .setTotalAmount(d.getAmount()).build())
                            .forEach(builder::addData);
                    return builder.build();
                })
                .recover(GrpcExceptionMapper::toFailedFuture);
    }
}