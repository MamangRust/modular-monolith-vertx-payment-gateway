package io.example.card.handler;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.service.CardStatsTransactionService;
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
public class CardStatsTransactionHandler
        implements pb.card.stats.VertxCardStatsTransactionServiceGrpcServer.CardStatsTransactionServiceApi {
    private final CardStatsTransactionService service;

    @Override
    public Future<ApiResponseMonthlyAmount> findMonthlyTransactionAmount(FindYearAmount req) {
        return service.getMonthlyTransactionAmount(req.getYear())
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
    public Future<ApiResponseYearlyAmount> findYearlyTransactionAmount(FindYearAmount req) {
        return service.getYearlyTransactionAmount(req.getYear())
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
    public Future<ApiResponseMonthlyAmount> findMonthlyTransactionAmountByCardNumber(FindYearAmountCardNumber req) {
        var reqDomain = MonthYearCardNumberCard.builder()
                .year(req.getYear())
                .cardNumber(req.getCardNumber())
                .build();

        return service.getMonthlyTransactionAmountByCardNumber(reqDomain)
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
    public Future<ApiResponseYearlyAmount> findYearlyTransactionAmountByCardNumber(FindYearAmountCardNumber req) {
        var reqDomain = MonthYearCardNumberCard.builder()
                .year(req.getYear())
                .cardNumber(req.getCardNumber())
                .build();

        return service.getYearlyTransactionAmountByCardNumber(reqDomain)
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