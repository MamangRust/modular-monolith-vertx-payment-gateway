package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import pb.transaction.Transaction;
import pb.transaction.TransactionCommand;
import pb.transaction.TransactionQuery;
import pb.transaction.VertxTransactionCommandServiceGrpcClient;
import pb.transaction.VertxTransactionQueryServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsAmountServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsMethodServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsStatusServiceGrpcClient;

@RequiredArgsConstructor
public class TransactionProxyHandler {
  private final VertxTransactionQueryServiceGrpcClient queryClient;
  private final VertxTransactionCommandServiceGrpcClient commandClient;
  private final VertxTransactionStatsAmountServiceGrpcClient statsAmountClient;
  private final VertxTransactionStatsMethodServiceGrpcClient statsMethodClient;
  private final VertxTransactionStatsStatusServiceGrpcClient statsStatusClient;

  private TransactionQuery.FindAllTransactionRequest buildFindAllTransactionReq(RoutingContext ctx) {
    return TransactionQuery.FindAllTransactionRequest.newBuilder()
        .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
        .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
        .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
        .build();
  }

  private Transaction.FindYearTransactionStatus buildFindYearTransactionStatusReq(RoutingContext ctx) {
    return Transaction.FindYearTransactionStatus.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .build();
  }

  private Transaction.FindByYearCardNumberTransactionRequest buildFindByYearCardNumberReq(RoutingContext ctx) {
    return Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  private Transaction.FindMonthlyTransactionStatus buildFindMonthlyTransactionStatusReq(RoutingContext ctx) {
    return Transaction.FindMonthlyTransactionStatus.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
        .build();
  }

  private Transaction.FindMonthlyTransactionStatusCardNumber buildFindMonthlyTransactionStatusCardNumberReq(
      RoutingContext ctx) {
    return Transaction.FindMonthlyTransactionStatusCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  private Transaction.FindYearTransactionStatusCardNumber buildFindYearTransactionStatusCardNumberReq(
      RoutingContext ctx) {
    return Transaction.FindYearTransactionStatusCardNumber.newBuilder()
        .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
        .setCardNumber(ctx.pathParam("cardNumber"))
        .build();
  }

  public void getTransactions(RoutingContext ctx) {
    queryClient.findAllTransaction(buildFindAllTransactionReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getActiveTransactions(RoutingContext ctx) {
    queryClient.findByActiveTransaction(buildFindAllTransactionReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTrashedTransactions(RoutingContext ctx) {
    queryClient.findByTrashedTransaction(buildFindAllTransactionReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTransactionById(RoutingContext ctx) {
    var req = Transaction.FindByIdTransactionRequest.newBuilder()
        .setTransactionId(GrpcGatewayUtils.getSafePathInt(ctx, "transactionId"))
        .build();
    queryClient.findByIdTransaction(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getTransactionsByCardNumber(RoutingContext ctx) {
    var req = TransactionQuery.FindAllTransactionCardNumberRequest.newBuilder()
        .setCardNumber(ctx.pathParam("cardNumber")).build();
    queryClient.findAllTransactionByCardNumber(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void createTransaction(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    pb.merchant.Merchant.MerchantResponse merchant = ctx.get("merchant");
    int merchantId = merchant != null ? merchant.getId() : 0;
    // ApiKeyMiddleware stores the validated key in the context; it must be
    // forwarded to the command service which re-resolves the merchant by key.
    String apiKey = ctx.get("apiKey");

    var req = TransactionCommand.CreateTransactionRequest.newBuilder()
        .setApiKey(apiKey != null ? apiKey : GrpcGatewayUtils.getJsonString(body, "api_key", ""))
        .setCardNumber(GrpcGatewayUtils.getJsonString(body, "card_number", ""))
        .setAmount(GrpcGatewayUtils.getJsonInteger(body, "amount", 0))
        .setPaymentMethod(GrpcGatewayUtils.getJsonString(body, "payment_method", ""))
        .setMerchantId(merchantId)
        .setIdempotencyKey(GrpcGatewayUtils.getJsonString(body, "idempotency_key", ""))
        .build();
    commandClient.createTransaction(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 201))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void updateTransaction(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    pb.merchant.Merchant.MerchantResponse merchant = ctx.get("merchant");
    int merchantId = merchant != null ? merchant.getId() : 0;
    String apiKey = ctx.get("apiKey");

    var req = TransactionCommand.UpdateTransactionRequest.newBuilder()
        .setTransactionId(GrpcGatewayUtils.getJsonInteger(body, "id", 0))
        .setApiKey(apiKey != null ? apiKey : GrpcGatewayUtils.getJsonString(body, "api_key", ""))
        .setCardNumber(GrpcGatewayUtils.getJsonString(body, "card_number", ""))
        .setAmount(GrpcGatewayUtils.getJsonInteger(body, "amount", 0))
        .setPaymentMethod(GrpcGatewayUtils.getJsonString(body, "payment_method", ""))
        .setMerchantId(merchantId)
        .build();
    commandClient.updateTransaction(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void trashTransaction(RoutingContext ctx) {
    var req = Transaction.FindByIdTransactionRequest.newBuilder()
        .setTransactionId(GrpcGatewayUtils.getSafePathInt(ctx, "transactionId"))
        .build();
    commandClient.trashedTransaction(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreTransaction(RoutingContext ctx) {
    var req = Transaction.FindByIdTransactionRequest.newBuilder()
        .setTransactionId(GrpcGatewayUtils.getSafePathInt(ctx, "transactionId"))
        .build();
    commandClient.restoreTransaction(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteTransactionPermanently(RoutingContext ctx) {
    var req = Transaction.FindByIdTransactionRequest.newBuilder()
        .setTransactionId(GrpcGatewayUtils.getSafePathInt(ctx, "transactionId"))
        .build();
    commandClient.deleteTransactionPermanent(req)
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void restoreAllTransactions(RoutingContext ctx) {
    commandClient.restoreAllTransaction(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void deleteAllPermanentTransactions(RoutingContext ctx) {
    commandClient.deleteAllTransactionPermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTransactionStatusSuccess(RoutingContext ctx) {
    statsStatusClient.findMonthlyTransactionStatusSuccess(buildFindMonthlyTransactionStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransactionStatusSuccess(RoutingContext ctx) {
    statsStatusClient.findYearlyTransactionStatusSuccess(buildFindYearTransactionStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTransactionStatusFailed(RoutingContext ctx) {
    statsStatusClient.findMonthlyTransactionStatusFailed(buildFindMonthlyTransactionStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransactionStatusFailed(RoutingContext ctx) {
    statsStatusClient.findYearlyTransactionStatusFailed(buildFindYearTransactionStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTransactionStatusSuccessCardNumber(RoutingContext ctx) {
    statsStatusClient
        .findMonthlyTransactionStatusSuccessByCardNumber(buildFindMonthlyTransactionStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransactionStatusSuccessCardNumber(RoutingContext ctx) {
    statsStatusClient.findYearlyTransactionStatusSuccessByCardNumber(buildFindYearTransactionStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthTransactionStatusFailedCardNumber(RoutingContext ctx) {
    statsStatusClient
        .findMonthlyTransactionStatusFailedByCardNumber(buildFindMonthlyTransactionStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyTransactionStatusFailedCardNumber(RoutingContext ctx) {
    statsStatusClient.findYearlyTransactionStatusFailedByCardNumber(buildFindYearTransactionStatusCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyPaymentMethods(RoutingContext ctx) {
    statsMethodClient.findMonthlyPaymentMethods(buildFindYearTransactionStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyPaymentMethods(RoutingContext ctx) {
    statsMethodClient.findYearlyPaymentMethods(buildFindYearTransactionStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyPaymentMethodsByCardNumber(RoutingContext ctx) {
    statsMethodClient.findMonthlyPaymentMethodsByCardNumber(buildFindByYearCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyPaymentMethodsByCardNumber(RoutingContext ctx) {
    statsMethodClient.findYearlyPaymentMethodsByCardNumber(buildFindByYearCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyAmounts(RoutingContext ctx) {
    statsAmountClient.findMonthlyAmounts(buildFindYearTransactionStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyAmounts(RoutingContext ctx) {
    statsAmountClient.findYearlyAmounts(buildFindYearTransactionStatusReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getMonthlyAmountsByCardNumber(RoutingContext ctx) {
    statsAmountClient.findMonthlyAmountsByCardNumber(buildFindByYearCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }

  public void getYearlyAmountsByCardNumber(RoutingContext ctx) {
    statsAmountClient.findYearlyAmountsByCardNumber(buildFindByYearCardNumberReq(ctx))
        .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
        .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
  }
}