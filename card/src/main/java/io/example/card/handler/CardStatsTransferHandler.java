package io.example.card.handler;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.service.CardStatsTransferService;
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
public class CardStatsTransferHandler
                implements pb.card.stats.VertxCardStatsTransferServiceGrpcServer.CardStatsTransferServiceApi {
        private final CardStatsTransferService service;

        @Override
        public Future<ApiResponseMonthlyAmount> findMonthlyTransferSenderAmount(FindYearAmount req) {
                return service.getMonthlyTransferAmountSender(req.getYear())
                                .map(list -> {
                                        var builder = ApiResponseMonthlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(d -> CardResponseMonthlyAmount.newBuilder()
                                                        .setMonth(String.valueOf(d.getMonth()))
                                                        .setTotalAmount(d.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMonthlyAmount> findMonthlyTransferReceiverAmount(FindYearAmount req) {
                return service.getMonthlyTransferAmountReceiver(req.getYear())
                                .map(list -> {
                                        var builder = ApiResponseMonthlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(d -> CardResponseMonthlyAmount.newBuilder()
                                                        .setMonth(String.valueOf(d.getMonth()))
                                                        .setTotalAmount(d.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseYearlyAmount> findYearlyTransferSenderAmount(FindYearAmount req) {
                return service.getYearlyTransferAmountSender(req.getYear())
                                .map(list -> {
                                        var builder = ApiResponseYearlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(d -> CardResponseYearlyAmount.newBuilder()
                                                        .setYear(String.valueOf(d.getYear()))
                                                        .setTotalAmount(d.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseYearlyAmount> findYearlyTransferReceiverAmount(FindYearAmount req) {
                return service.getYearlyTransferAmountReceiver(req.getYear())
                                .map(list -> {
                                        var builder = ApiResponseYearlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(d -> CardResponseYearlyAmount.newBuilder()
                                                        .setYear(String.valueOf(d.getYear()))
                                                        .setTotalAmount(d.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMonthlyAmount> findMonthlyTransferSenderAmountByCardNumber(
                        FindYearAmountCardNumber req) {
                var reqDomain = MonthYearCardNumberCard.builder()
                                .year(req.getYear())
                                .cardNumber(req.getCardNumber())
                                .build();

                return service.getMonthlyTransferAmountBySender(reqDomain)
                                .map(list -> {
                                        var builder = ApiResponseMonthlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(d -> CardResponseMonthlyAmount.newBuilder()
                                                        .setMonth(String.valueOf(d.getMonth()))
                                                        .setTotalAmount(d.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMonthlyAmount> findMonthlyTransferReceiverAmountByCardNumber(
                        FindYearAmountCardNumber req) {
                var reqDomain = MonthYearCardNumberCard.builder()
                                .year(req.getYear())
                                .cardNumber(req.getCardNumber())
                                .build();

                return service.getMonthlyTransferAmountByReceiver(reqDomain)
                                .map(list -> {
                                        var builder = ApiResponseMonthlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(d -> CardResponseMonthlyAmount.newBuilder()
                                                        .setMonth(String.valueOf(d.getMonth()))
                                                        .setTotalAmount(d.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseYearlyAmount> findYearlyTransferSenderAmountByCardNumber(
                        FindYearAmountCardNumber req) {
                var reqDomain = MonthYearCardNumberCard.builder()
                                .year(req.getYear())
                                .cardNumber(req.getCardNumber())
                                .build();

                return service.getYearlyTransferAmountBySender(reqDomain)
                                .map(list -> {
                                        var builder = ApiResponseYearlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(d -> CardResponseYearlyAmount.newBuilder()
                                                        .setYear(String.valueOf(d.getYear()))
                                                        .setTotalAmount(d.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseYearlyAmount> findYearlyTransferReceiverAmountByCardNumber(
                        FindYearAmountCardNumber req) {
                var reqDomain = MonthYearCardNumberCard.builder()
                                .year(req.getYear())
                                .cardNumber(req.getCardNumber())
                                .build();

                return service.getYearlyTransferAmountByReceiver(reqDomain)
                                .map(list -> {
                                        var builder = ApiResponseYearlyAmount.newBuilder().setStatus("success")
                                                        .setMessage("OK");
                                        list.stream().map(d -> CardResponseYearlyAmount.newBuilder()
                                                        .setYear(String.valueOf(d.getYear()))
                                                        .setTotalAmount(d.getAmount()).build())
                                                        .forEach(builder::addData);
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}