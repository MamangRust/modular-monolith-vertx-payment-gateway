package io.example.transaction.handler;

import io.example.transaction.service.TransactionStatsMethodService;
import io.vertx.core.Future;
import pb.transaction.Transaction;
import pb.transaction.stats.TransactionStatsMethod;
import pb.transaction.stats.VertxTransactionStatsMethodServiceGrpcServer;

public class TransactionStatsMethodHandler
        implements VertxTransactionStatsMethodServiceGrpcServer.TransactionStatsMethodServiceApi {
    private final TransactionStatsMethodService service;

    public TransactionStatsMethodHandler(TransactionStatsMethodService service) {
        this.service = service;
    }

    @Override
    public Future<TransactionStatsMethod.ApiResponseTransactionMonthMethod> findMonthlyPaymentMethods(
            Transaction.FindYearTransactionStatus request) {
        return service.getMonthlyMethods(request)
                .map(list -> {
                    TransactionStatsMethod.ApiResponseTransactionMonthMethod.Builder builder = TransactionStatsMethod.ApiResponseTransactionMonthMethod
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Monthly methods retrieved");
                    for (var m : list) {
                        builder.addData(TransactionStatsMethod.TransactionMonthMethodResponse.newBuilder()
                                .setMonth(m.getMonth())
                                .setPaymentMethod(m.getPaymentMethod())
                                .setTotalTransactions(m.getTotalTransactions().intValue())
                                .setTotalAmount(m.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsMethod.ApiResponseTransactionYearMethod> findYearlyPaymentMethods(
            Transaction.FindYearTransactionStatus request) {
        return service.getYearlyMethods(request)
                .map(list -> {
                    TransactionStatsMethod.ApiResponseTransactionYearMethod.Builder builder = TransactionStatsMethod.ApiResponseTransactionYearMethod
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Yearly methods retrieved");
                    for (var y : list) {
                        builder.addData(TransactionStatsMethod.TransactionYearMethodResponse.newBuilder()
                                .setYear(String.valueOf(y.getYear()))
                                .setPaymentMethod(y.getPaymentMethod())
                                .setTotalTransactions(y.getTotalTransactions().intValue())
                                .setTotalAmount(y.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsMethod.ApiResponseTransactionMonthMethod> findMonthlyPaymentMethodsByCardNumber(
            Transaction.FindByYearCardNumberTransactionRequest request) {
        return service.getMonthlyMethodsByCard(request)
                .map(list -> {
                    TransactionStatsMethod.ApiResponseTransactionMonthMethod.Builder builder = TransactionStatsMethod.ApiResponseTransactionMonthMethod
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Monthly methods by card retrieved");
                    for (var m : list) {
                        builder.addData(TransactionStatsMethod.TransactionMonthMethodResponse.newBuilder()
                                .setMonth(m.getMonth())
                                .setPaymentMethod(m.getPaymentMethod())
                                .setTotalTransactions(m.getTotalTransactions().intValue())
                                .setTotalAmount(m.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<TransactionStatsMethod.ApiResponseTransactionYearMethod> findYearlyPaymentMethodsByCardNumber(
            Transaction.FindByYearCardNumberTransactionRequest request) {
        return service.getYearlyMethodsByCard(request)
                .map(list -> {
                    TransactionStatsMethod.ApiResponseTransactionYearMethod.Builder builder = TransactionStatsMethod.ApiResponseTransactionYearMethod
                            .newBuilder()
                            .setStatus("200")
                            .setMessage("Yearly methods by card retrieved");
                    for (var y : list) {
                        builder.addData(TransactionStatsMethod.TransactionYearMethodResponse.newBuilder()
                                .setYear(String.valueOf(y.getYear()))
                                .setPaymentMethod(y.getPaymentMethod())
                                .setTotalTransactions(y.getTotalTransactions().intValue())
                                .setTotalAmount(y.getTotalAmount().intValue())
                                .build());
                    }
                    return builder.build();
                });
    }
}
