package io.example.transaction.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transaction.domain.requests.YearCardNumberTransactionRequest;
import io.example.transaction.domain.requests.YearTransactionRequest;
import io.example.transaction.service.TransactionStatsMethodService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transaction.Transaction;
import pb.transaction.stats.TransactionStatsMethod;
import pb.transaction.stats.VertxTransactionStatsMethodServiceGrpcServer;

@RequiredArgsConstructor
public class TransactionStatsMethodHandler
                implements VertxTransactionStatsMethodServiceGrpcServer.TransactionStatsMethodServiceApi {
        private final TransactionStatsMethodService service;

        @Override
        public Future<TransactionStatsMethod.ApiResponseTransactionMonthMethod> findMonthlyPaymentMethods(
                        Transaction.FindYearTransactionStatus req) {
                var domainReq = YearTransactionRequest.builder().year(req.getYear()).build();
                return service.getMonthlyMethods(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsMethod.ApiResponseTransactionMonthMethod
                                                        .newBuilder()
                                                        .setStatus("success").setMessage("OK");
                                        for (var m : list) {
                                                builder.addData(TransactionStatsMethod.TransactionMonthMethodResponse
                                                                .newBuilder()
                                                                .setMonth(m.getMonth())
                                                                .setPaymentMethod(m.getPaymentMethod())
                                                                .setTotalTransactions(
                                                                                m.getTotalTransactions().intValue())
                                                                .setTotalAmount(m.getTotalAmount().intValue())
                                                                .build());
                                        }
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsMethod.ApiResponseTransactionYearMethod> findYearlyPaymentMethods(
                        Transaction.FindYearTransactionStatus req) {
                var domainReq = YearTransactionRequest.builder().year(req.getYear()).build();
                return service.getYearlyMethods(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsMethod.ApiResponseTransactionYearMethod
                                                        .newBuilder()
                                                        .setStatus("success").setMessage("OK");
                                        for (var y : list) {
                                                builder.addData(TransactionStatsMethod.TransactionYearMethodResponse
                                                                .newBuilder()
                                                                .setYear(String.valueOf(y.getYear()))
                                                                .setPaymentMethod(y.getPaymentMethod())
                                                                .setTotalTransactions(
                                                                                y.getTotalTransactions().intValue())
                                                                .setTotalAmount(y.getTotalAmount().intValue())
                                                                .build());
                                        }
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsMethod.ApiResponseTransactionMonthMethod> findMonthlyPaymentMethodsByCardNumber(
                        Transaction.FindByYearCardNumberTransactionRequest req) {
                var domainReq = YearCardNumberTransactionRequest.builder().cardNumber(req.getCardNumber())
                                .year(req.getYear()).build();
                return service.getMonthlyMethodsByCard(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsMethod.ApiResponseTransactionMonthMethod
                                                        .newBuilder()
                                                        .setStatus("success").setMessage("OK");
                                        for (var m : list) {
                                                builder.addData(TransactionStatsMethod.TransactionMonthMethodResponse
                                                                .newBuilder()
                                                                .setMonth(m.getMonth())
                                                                .setPaymentMethod(m.getPaymentMethod())
                                                                .setTotalTransactions(
                                                                                m.getTotalTransactions().intValue())
                                                                .setTotalAmount(m.getTotalAmount().intValue())
                                                                .build());
                                        }
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsMethod.ApiResponseTransactionYearMethod> findYearlyPaymentMethodsByCardNumber(
                        Transaction.FindByYearCardNumberTransactionRequest req) {
                var domainReq = YearCardNumberTransactionRequest.builder().cardNumber(req.getCardNumber())
                                .year(req.getYear()).build();
                return service.getYearlyMethodsByCard(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsMethod.ApiResponseTransactionYearMethod
                                                        .newBuilder()
                                                        .setStatus("success").setMessage("OK");
                                        for (var y : list) {
                                                builder.addData(TransactionStatsMethod.TransactionYearMethodResponse
                                                                .newBuilder()
                                                                .setYear(String.valueOf(y.getYear()))
                                                                .setPaymentMethod(y.getPaymentMethod())
                                                                .setTotalTransactions(
                                                                                y.getTotalTransactions().intValue())
                                                                .setTotalAmount(y.getTotalAmount().intValue())
                                                                .build());
                                        }
                                        return builder.build();
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}