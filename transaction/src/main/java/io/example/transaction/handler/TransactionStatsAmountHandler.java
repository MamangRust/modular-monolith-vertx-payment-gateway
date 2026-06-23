package io.example.transaction.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transaction.domain.requests.YearCardNumberTransactionRequest;
import io.example.transaction.domain.requests.YearTransactionRequest;
import io.example.transaction.service.TransactionStatsAmountService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transaction.Transaction;
import pb.transaction.stats.VertxTransactionStatsAmountServiceGrpcServer;
import pb.transaction.stats.TransactionStatsAmount;

@RequiredArgsConstructor
public class TransactionStatsAmountHandler
                implements VertxTransactionStatsAmountServiceGrpcServer.TransactionStatsAmountServiceApi {
        private final TransactionStatsAmountService service;

        @Override
        public Future<TransactionStatsAmount.ApiResponseTransactionMonthAmount> findMonthlyAmounts(
                        Transaction.FindYearTransactionStatus req) {
                var domainReq = YearTransactionRequest.builder().year(req.getYear()).build();
                return service.getMonthlyAmounts(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsAmount.ApiResponseTransactionMonthAmount
                                                        .newBuilder()
                                                        .setStatus("success").setMessage("OK");
                                        for (var m : list) {
                                                builder.addData(TransactionStatsAmount.TransactionMonthAmountResponse
                                                                .newBuilder()
                                                                .setMonth(m.getMonth())
                                                                .setTotalAmount(m.getTotalAmount().intValue()).build());
                                        }
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsAmount.ApiResponseTransactionYearAmount> findYearlyAmounts(
                        Transaction.FindYearTransactionStatus req) {
                var domainReq = YearTransactionRequest.builder().year(req.getYear()).build();
                return service.getYearlyAmounts(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsAmount.ApiResponseTransactionYearAmount
                                                        .newBuilder()
                                                        .setStatus("success").setMessage("OK");
                                        for (var y : list) {
                                                builder.addData(TransactionStatsAmount.TransactionYearlyAmountResponse
                                                                .newBuilder()
                                                                .setYear(String.valueOf(y.getYear()))
                                                                .setTotalAmount(y.getTotalAmount().intValue()).build());
                                        }
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsAmount.ApiResponseTransactionMonthAmount> findMonthlyAmountsByCardNumber(
                        Transaction.FindByYearCardNumberTransactionRequest req) {
                var domainReq = YearCardNumberTransactionRequest.builder().cardNumber(req.getCardNumber())
                                .year(req.getYear()).build();
                return service.getMonthlyAmountsByCard(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsAmount.ApiResponseTransactionMonthAmount
                                                        .newBuilder()
                                                        .setStatus("success").setMessage("OK");
                                        for (var m : list) {
                                                builder.addData(TransactionStatsAmount.TransactionMonthAmountResponse
                                                                .newBuilder()
                                                                .setMonth(m.getMonth())
                                                                .setTotalAmount(m.getTotalAmount().intValue()).build());
                                        }
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsAmount.ApiResponseTransactionYearAmount> findYearlyAmountsByCardNumber(
                        Transaction.FindByYearCardNumberTransactionRequest req) {
                var domainReq = YearCardNumberTransactionRequest.builder().cardNumber(req.getCardNumber())
                                .year(req.getYear()).build();
                return service.getYearlyAmountsByCard(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsAmount.ApiResponseTransactionYearAmount
                                                        .newBuilder()
                                                        .setStatus("success").setMessage("OK");
                                        for (var y : list) {
                                                builder.addData(TransactionStatsAmount.TransactionYearlyAmountResponse
                                                                .newBuilder()
                                                                .setYear(String.valueOf(y.getYear()))
                                                                .setTotalAmount(y.getTotalAmount().intValue()).build());
                                        }
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}