package io.example.transaction.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transaction.domain.requests.MonthStatusTransaction;
import io.example.transaction.domain.requests.MonthStatusTransactionCardNumber;
import io.example.transaction.domain.requests.YearStatusTransaction;
import io.example.transaction.domain.requests.YearStatusTransactionCardNumber;
import io.example.transaction.service.TransactionStatsStatusService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transaction.Transaction;
import pb.transaction.stats.TransactionStatsStatus;
import pb.transaction.stats.VertxTransactionStatsStatusServiceGrpcServer;

@RequiredArgsConstructor
public class TransactionStatsStatusHandler
                implements VertxTransactionStatsStatusServiceGrpcServer.TransactionStatsStatusServiceApi {
        private final TransactionStatsStatusService service;

        @Override
        public Future<TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess> findMonthlyTransactionStatusSuccess(
                        Transaction.FindMonthlyTransactionStatus req) {
                var domainReq = MonthStatusTransaction.builder().year(req.getYear()).month(req.getMonth())
                                .status("success").build();
                return service.getMonthlyStatus(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess
                                                        .newBuilder().setStatus("success").setMessage("OK");
                                        for (var m : list)
                                                builder.addData(TransactionStatsStatus.TransactionMonthStatusSuccessResponse
                                                                .newBuilder().setYear(String.valueOf(m.getYear()))
                                                                .setMonth(m.getMonth())
                                                                .setTotalSuccess(m.getTotalCount().intValue())
                                                                .setTotalAmount(m.getTotalAmount().intValue()).build());
                                        return builder.build();
                                }).recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess> findYearlyTransactionStatusSuccess(
                        Transaction.FindYearTransactionStatus req) {
                var domainReq = YearStatusTransaction.builder().year(req.getYear()).status("success").build();
                return service.getYearlyStatus(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess
                                                        .newBuilder().setStatus("success").setMessage("OK");
                                        for (var y : list)
                                                builder.addData(TransactionStatsStatus.TransactionYearStatusSuccessResponse
                                                                .newBuilder().setYear(String.valueOf(y.getYear()))
                                                                .setTotalSuccess(y.getTotalCount().intValue())
                                                                .setTotalAmount(y.getTotalAmount().intValue()).build());
                                        return builder.build();
                                }).recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed> findMonthlyTransactionStatusFailed(
                        Transaction.FindMonthlyTransactionStatus req) {
                var domainReq = MonthStatusTransaction.builder().year(req.getYear()).month(req.getMonth())
                                .status("failed").build();
                return service.getMonthlyStatus(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed
                                                        .newBuilder().setStatus("success").setMessage("OK");
                                        for (var m : list)
                                                builder.addData(TransactionStatsStatus.TransactionMonthStatusFailedResponse
                                                                .newBuilder().setYear(String.valueOf(m.getYear()))
                                                                .setMonth(m.getMonth())
                                                                .setTotalFailed(m.getTotalCount().intValue())
                                                                .setTotalAmount(m.getTotalAmount().intValue()).build());
                                        return builder.build();
                                }).recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsStatus.ApiResponseTransactionYearStatusFailed> findYearlyTransactionStatusFailed(
                        Transaction.FindYearTransactionStatus req) {
                var domainReq = YearStatusTransaction.builder().year(req.getYear()).status("failed").build();
                return service.getYearlyStatus(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsStatus.ApiResponseTransactionYearStatusFailed
                                                        .newBuilder().setStatus("success").setMessage("OK");
                                        for (var y : list)
                                                builder.addData(TransactionStatsStatus.TransactionYearStatusFailedResponse
                                                                .newBuilder().setYear(String.valueOf(y.getYear()))
                                                                .setTotalFailed(y.getTotalCount().intValue())
                                                                .setTotalAmount(y.getTotalAmount().intValue()).build());
                                        return builder.build();
                                }).recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess> findMonthlyTransactionStatusSuccessByCardNumber(
                        Transaction.FindMonthlyTransactionStatusCardNumber req) {
                var domainReq = MonthStatusTransactionCardNumber.builder().cardNumber(req.getCardNumber())
                                .year(req.getYear()).month(req.getMonth()).status("success").build();
                return service.getMonthlyStatusByCard(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess
                                                        .newBuilder().setStatus("success").setMessage("OK");
                                        for (var m : list)
                                                builder.addData(TransactionStatsStatus.TransactionMonthStatusSuccessResponse
                                                                .newBuilder().setYear(String.valueOf(m.getYear()))
                                                                .setMonth(m.getMonth())
                                                                .setTotalSuccess(m.getTotalCount().intValue())
                                                                .setTotalAmount(m.getTotalAmount().intValue()).build());
                                        return builder.build();
                                }).recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess> findYearlyTransactionStatusSuccessByCardNumber(
                        Transaction.FindYearTransactionStatusCardNumber req) {
                var domainReq = YearStatusTransactionCardNumber.builder().cardNumber(req.getCardNumber())
                                .year(req.getYear()).status("success").build();
                return service.getYearlyStatusByCard(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess
                                                        .newBuilder().setStatus("success").setMessage("OK");
                                        for (var y : list)
                                                builder.addData(TransactionStatsStatus.TransactionYearStatusSuccessResponse
                                                                .newBuilder().setYear(String.valueOf(y.getYear()))
                                                                .setTotalSuccess(y.getTotalCount().intValue())
                                                                .setTotalAmount(y.getTotalAmount().intValue()).build());
                                        return builder.build();
                                }).recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed> findMonthlyTransactionStatusFailedByCardNumber(
                        Transaction.FindMonthlyTransactionStatusCardNumber req) {
                var domainReq = MonthStatusTransactionCardNumber.builder().cardNumber(req.getCardNumber())
                                .year(req.getYear()).month(req.getMonth()).status("failed").build();
                return service.getMonthlyStatusByCard(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed
                                                        .newBuilder().setStatus("success").setMessage("OK");
                                        for (var m : list)
                                                builder.addData(TransactionStatsStatus.TransactionMonthStatusFailedResponse
                                                                .newBuilder().setYear(String.valueOf(m.getYear()))
                                                                .setMonth(m.getMonth())
                                                                .setTotalFailed(m.getTotalCount().intValue())
                                                                .setTotalAmount(m.getTotalAmount().intValue()).build());
                                        return builder.build();
                                }).recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<TransactionStatsStatus.ApiResponseTransactionYearStatusFailed> findYearlyTransactionStatusFailedByCardNumber(
                        Transaction.FindYearTransactionStatusCardNumber req) {
                var domainReq = YearStatusTransactionCardNumber.builder().cardNumber(req.getCardNumber())
                                .year(req.getYear()).status("failed").build();
                return service.getYearlyStatusByCard(domainReq)
                                .map(list -> {
                                        var builder = TransactionStatsStatus.ApiResponseTransactionYearStatusFailed
                                                        .newBuilder().setStatus("success").setMessage("OK");
                                        for (var y : list)
                                                builder.addData(TransactionStatsStatus.TransactionYearStatusFailedResponse
                                                                .newBuilder().setYear(String.valueOf(y.getYear()))
                                                                .setTotalFailed(y.getTotalCount().intValue())
                                                                .setTotalAmount(y.getTotalAmount().intValue()).build());
                                        return builder.build();
                                }).recover(GrpcExceptionMapper::toFailedFuture);
        }
}