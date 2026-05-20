package io.example.transaction.handler;

import io.example.transaction.service.TransactionStatsAmountService;
import io.vertx.core.Future;
import pb.transaction.Transaction;
import pb.transaction.stats.TransactionStatsAmount;
import pb.transaction.stats.VertxTransactionStatsAmountServiceGrpcServer;

public class TransactionStatsAmountHandler
        implements VertxTransactionStatsAmountServiceGrpcServer.TransactionStatsAmountServiceApi {
    private final TransactionStatsAmountService service;

    public TransactionStatsAmountHandler(TransactionStatsAmountService service) {
        this.service = service;
    }

    @Override
    public Future<TransactionStatsAmount.ApiResponseTransactionMonthAmount> findMonthlyAmounts(
            Transaction.FindYearTransactionStatus request) {
        return service.getMonthlyAmounts(request)
                .map(list -> {
                    TransactionStatsAmount.ApiResponseTransactionMonthAmount.Builder builder = TransactionStatsAmount.ApiResponseTransactionMonthAmount
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Monthly amounts retrieved");
                    for (var m : list) {
                        builder.addData(TransactionStatsAmount.TransactionMonthAmountResponse.newBuilder()
                                .setMonth(m.getMonth())
                                .setTotalAmount(m.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsAmount.ApiResponseTransactionYearAmount> findYearlyAmounts(
            Transaction.FindYearTransactionStatus request) {
        return service.getYearlyAmounts(request)
                .map(list -> {
                    TransactionStatsAmount.ApiResponseTransactionYearAmount.Builder builder = TransactionStatsAmount.ApiResponseTransactionYearAmount
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Yearly amounts retrieved");
                    for (var y : list) {
                        builder.addData(TransactionStatsAmount.TransactionYearlyAmountResponse.newBuilder()
                                .setYear(String.valueOf(y.getYear()))
                                .setTotalAmount(y.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsAmount.ApiResponseTransactionMonthAmount> findMonthlyAmountsByCardNumber(
            Transaction.FindByYearCardNumberTransactionRequest request) {
        return service.getMonthlyAmountsByCard(request)
                .map(list -> {
                    TransactionStatsAmount.ApiResponseTransactionMonthAmount.Builder builder = TransactionStatsAmount.ApiResponseTransactionMonthAmount
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Monthly amounts by card retrieved");
                    for (var m : list) {
                        builder.addData(TransactionStatsAmount.TransactionMonthAmountResponse.newBuilder()
                                .setMonth(m.getMonth())
                                .setTotalAmount(m.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsAmount.ApiResponseTransactionYearAmount> findYearlyAmountsByCardNumber(
            Transaction.FindByYearCardNumberTransactionRequest request) {
        return service.getYearlyAmountsByCard(request)
                .map(list -> {
                    TransactionStatsAmount.ApiResponseTransactionYearAmount.Builder builder = TransactionStatsAmount.ApiResponseTransactionYearAmount
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Yearly amounts by card retrieved");
                    for (var y : list) {
                        builder.addData(TransactionStatsAmount.TransactionYearlyAmountResponse.newBuilder()
                                .setYear(String.valueOf(y.getYear()))
                                .setTotalAmount(y.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }
}
