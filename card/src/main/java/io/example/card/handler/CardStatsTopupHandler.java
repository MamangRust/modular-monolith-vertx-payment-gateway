package io.example.card.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.service.CardStatsTopupService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.Card.ApiResponseMonthlyAmount;
import pb.card.Card.ApiResponseYearlyAmount;
import pb.card.Card.CardResponseMonthlyAmount;
import pb.card.Card.CardResponseYearlyAmount;
import pb.card.Card.FindYearAmount;
import pb.card.Card.FindYearAmountCardNumber;

@RequiredArgsConstructor
public class CardStatsTopupHandler
        implements pb.card.stats.VertxCardStatsTopupServiceGrpcServer.CardStatsTopupServiceApi {
    private final CardStatsTopupService service;

    @Override
    public Future<ApiResponseMonthlyAmount> findMonthlyTopupAmount(FindYearAmount req) {
        return service.getMonthlyTopupAmount(req.getYear())
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
    public Future<ApiResponseYearlyAmount> findYearlyTopupAmount(FindYearAmount req) {
        return service.getYearlyTopupAmount(req.getYear())
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
    public Future<ApiResponseMonthlyAmount> findMonthlyTopupAmountByCardNumber(FindYearAmountCardNumber req) {
        var reqDomain = MonthYearCardNumberCard.builder()
                .year(req.getYear())
                .cardNumber(req.getCardNumber())
                .build();

        return service.getMonthlyTopupAmountByCardNumber(reqDomain)
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
    public Future<ApiResponseYearlyAmount> findYearlyTopupAmountByCardNumber(FindYearAmountCardNumber req) {
        var reqDomain = MonthYearCardNumberCard.builder()
                .year(req.getYear())
                .cardNumber(req.getCardNumber())
                .build();

        return service.getYearlyTopupAmountByCardNumber(reqDomain)
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