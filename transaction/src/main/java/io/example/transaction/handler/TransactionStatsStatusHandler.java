package io.example.transaction.handler;

import io.example.transaction.service.TransactionStatsStatusService;
import io.vertx.core.Future;
import pb.transaction.Transaction;
import pb.transaction.stats.TransactionStatsStatus;
import pb.transaction.stats.VertxTransactionStatsStatusServiceGrpcServer;

public class TransactionStatsStatusHandler
        implements VertxTransactionStatsStatusServiceGrpcServer.TransactionStatsStatusServiceApi {
    private final TransactionStatsStatusService service;

    public TransactionStatsStatusHandler(TransactionStatsStatusService service) {
        this.service = service;
    }

    @Override
    public Future<TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess> findMonthlyTransactionStatusSuccess(
            Transaction.FindMonthlyTransactionStatus request) {
        return service.getMonthlyStatus(request, "success")
                .map(list -> {
                    TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess.Builder builder = TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Monthly success status retrieved");
                    for (var m : list) {
                        builder.addData(TransactionStatsStatus.TransactionMonthStatusSuccessResponse.newBuilder()
                                .setYear(String.valueOf(m.getYear()))
                                .setMonth(m.getMonth())
                                .setTotalSuccess(m.getTotalCount().intValue())
                                .setTotalAmount(m.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess> findYearlyTransactionStatusSuccess(
            Transaction.FindYearTransactionStatus request) {
        return service.getYearlyStatus(request, "success")
                .map(list -> {
                    TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess.Builder builder = TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Yearly success status retrieved");
                    for (var y : list) {
                        builder.addData(TransactionStatsStatus.TransactionYearStatusSuccessResponse.newBuilder()
                                .setYear(String.valueOf(y.getYear()))
                                .setTotalSuccess(y.getTotalCount().intValue())
                                .setTotalAmount(y.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed> findMonthlyTransactionStatusFailed(
            Transaction.FindMonthlyTransactionStatus request) {
        return service.getMonthlyStatus(request, "failed")
                .map(list -> {
                    TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed.Builder builder = TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Monthly failed status retrieved");
                    for (var m : list) {
                        builder.addData(TransactionStatsStatus.TransactionMonthStatusFailedResponse.newBuilder()
                                .setYear(String.valueOf(m.getYear()))
                                .setMonth(m.getMonth())
                                .setTotalFailed(m.getTotalCount().intValue())
                                .setTotalAmount(m.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsStatus.ApiResponseTransactionYearStatusFailed> findYearlyTransactionStatusFailed(
            Transaction.FindYearTransactionStatus request) {
        return service.getYearlyStatus(request, "failed")
                .map(list -> {
                    TransactionStatsStatus.ApiResponseTransactionYearStatusFailed.Builder builder = TransactionStatsStatus.ApiResponseTransactionYearStatusFailed
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Yearly failed status retrieved");
                    for (var y : list) {
                        builder.addData(TransactionStatsStatus.TransactionYearStatusFailedResponse.newBuilder()
                                .setYear(String.valueOf(y.getYear()))
                                .setTotalFailed(y.getTotalCount().intValue())
                                .setTotalAmount(y.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess> findMonthlyTransactionStatusSuccessByCardNumber(
            Transaction.FindMonthlyTransactionStatusCardNumber request) {
        return service.getMonthlyStatusByCard(request, "success")
                .map(list -> {
                    TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess.Builder builder = TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Monthly success status by card retrieved");
                    for (var m : list) {
                        builder.addData(TransactionStatsStatus.TransactionMonthStatusSuccessResponse.newBuilder()
                                .setYear(String.valueOf(m.getYear()))
                                .setMonth(m.getMonth())
                                .setTotalSuccess(m.getTotalCount().intValue())
                                .setTotalAmount(m.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess> findYearlyTransactionStatusSuccessByCardNumber(
            Transaction.FindYearTransactionStatusCardNumber request) {
        return service.getYearlyStatusByCard(request, "success")
                .map(list -> {
                    TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess.Builder builder = TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Yearly success status by card retrieved");
                    for (var y : list) {
                        builder.addData(TransactionStatsStatus.TransactionYearStatusSuccessResponse.newBuilder()
                                .setYear(String.valueOf(y.getYear()))
                                .setTotalSuccess(y.getTotalCount().intValue())
                                .setTotalAmount(y.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed> findMonthlyTransactionStatusFailedByCardNumber(
            Transaction.FindMonthlyTransactionStatusCardNumber request) {
        return service.getMonthlyStatusByCard(request, "failed")
                .map(list -> {
                    TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed.Builder builder = TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Monthly failed status by card retrieved");
                    for (var m : list) {
                        builder.addData(TransactionStatsStatus.TransactionMonthStatusFailedResponse.newBuilder()
                                .setYear(String.valueOf(m.getYear()))
                                .setMonth(m.getMonth())
                                .setTotalFailed(m.getTotalCount().intValue())
                                .setTotalAmount(m.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsStatus.ApiResponseTransactionYearStatusFailed> findYearlyTransactionStatusFailedByCardNumber(
            Transaction.FindYearTransactionStatusCardNumber request) {
        return service.getYearlyStatusByCard(request, "failed")
                .map(list -> {
                    TransactionStatsStatus.ApiResponseTransactionYearStatusFailed.Builder builder = TransactionStatsStatus.ApiResponseTransactionYearStatusFailed
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Yearly failed status by card retrieved");
                    for (var y : list) {
                        builder.addData(TransactionStatsStatus.TransactionYearStatusFailedResponse.newBuilder()
                                .setYear(String.valueOf(y.getYear()))
                                .setTotalFailed(y.getTotalCount().intValue())
                                .setTotalAmount(y.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }
}
