package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.transaction.Transaction;
import pb.transaction.TransactionCommand;
import pb.transaction.TransactionQuery;
import pb.transaction.VertxTransactionCommandServiceGrpcClient;
import pb.transaction.VertxTransactionQueryServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsAmountServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsMethodServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsStatusServiceGrpcClient;

public class TransactionProxyHandler {
  private final VertxTransactionQueryServiceGrpcClient queryClient;
  private final VertxTransactionCommandServiceGrpcClient commandClient;
  private final VertxTransactionStatsAmountServiceGrpcClient statsAmountClient;
  private final VertxTransactionStatsMethodServiceGrpcClient statsMethodClient;
  private final VertxTransactionStatsStatusServiceGrpcClient statsStatusClient;

  public TransactionProxyHandler(
      VertxTransactionQueryServiceGrpcClient queryClient,
      VertxTransactionCommandServiceGrpcClient commandClient,
      VertxTransactionStatsAmountServiceGrpcClient statsAmountClient,
      VertxTransactionStatsMethodServiceGrpcClient statsMethodClient,
      VertxTransactionStatsStatusServiceGrpcClient statsStatusClient) {
    this.queryClient = queryClient;
    this.commandClient = commandClient;
    this.statsAmountClient = statsAmountClient;
    this.statsMethodClient = statsMethodClient;
    this.statsStatusClient = statsStatusClient;
  }

  private int getYearParam(RoutingContext ctx) {
    return ctx.queryParams().contains("year") ? Integer.parseInt(ctx.queryParams().get("year")) : 2024;
  }
  private int getMonthParam(RoutingContext ctx) {
    return ctx.queryParams().contains("month") ? Integer.parseInt(ctx.queryParams().get("month")) : 1;
  }

  // == BASIC ==
  public void getTransactions(RoutingContext ctx) {
    var req = TransactionQuery.FindAllTransactionRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findAllTransaction(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getActiveTransactions(RoutingContext ctx) {
    var req = TransactionQuery.FindAllTransactionRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByActiveTransaction(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTrashedTransactions(RoutingContext ctx) {
    var req = TransactionQuery.FindAllTransactionRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByTrashedTransaction(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTransactionById(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("transactionId"));
    var req = Transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(id).build();
    queryClient.findByIdTransaction(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTransactionsByCardNumber(RoutingContext ctx) {
    String card = ctx.pathParam("cardNumber");
    var req = TransactionQuery.FindAllTransactionCardNumberRequest.newBuilder().setCardNumber(card).build();
    queryClient.findAllTransactionByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == COMMANDS ==
  public void createTransaction(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    // Retrieve merchantId from validation context
    pb.merchant.Merchant.MerchantResponse merchant = ctx.get("merchant");
    int merchantId = merchant != null ? merchant.getId() : 0;

    var req = TransactionCommand.CreateTransactionRequest.newBuilder()
        .setCardNumber(body.getString("card_number", ""))
        .setAmount(body.getInteger("amount", 0))
        .setPaymentMethod(body.getString("payment_method", ""))
        .setMerchantId(merchantId)
        .build();
    commandClient.createTransaction(req).onSuccess(r -> sendResponse(ctx, r, 201)).onFailure(ctx::fail);
  }

  public void updateTransaction(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    pb.merchant.Merchant.MerchantResponse merchant = ctx.get("merchant");
    int merchantId = merchant != null ? merchant.getId() : 0;

    var req = TransactionCommand.UpdateTransactionRequest.newBuilder()
        .setTransactionId(body.getInteger("id", 0))
        .setCardNumber(body.getString("card_number", ""))
        .setAmount(body.getInteger("amount", 0))
        .setPaymentMethod(body.getString("payment_method", ""))
        .setMerchantId(merchantId)
        .build();
    commandClient.updateTransaction(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void trashTransaction(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("transactionId"));
    var req = Transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(id).build();
    commandClient.trashedTransaction(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreTransaction(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("transactionId"));
    var req = Transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(id).build();
    commandClient.restoreTransaction(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteTransactionPermanently(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("transactionId"));
    var req = Transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(id).build();
    commandClient.deleteTransactionPermanent(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreAllTransactions(RoutingContext ctx) {
    commandClient.restoreAllTransaction(com.google.protobuf.Empty.getDefaultInstance()).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteAllPermanentTransactions(RoutingContext ctx) {
    commandClient.deleteAllTransactionPermanent(com.google.protobuf.Empty.getDefaultInstance()).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS STATUS ==
  public void getMonthTransactionStatusSuccess(RoutingContext ctx) {
    var req = Transaction.FindMonthlyTransactionStatus.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).build();
    statsStatusClient.findMonthlyTransactionStatusSuccess(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransactionStatusSuccess(RoutingContext ctx) {
    var req = Transaction.FindYearTransactionStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsStatusClient.findYearlyTransactionStatusSuccess(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthTransactionStatusFailed(RoutingContext ctx) {
    var req = Transaction.FindMonthlyTransactionStatus.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).build();
    statsStatusClient.findMonthlyTransactionStatusFailed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransactionStatusFailed(RoutingContext ctx) {
    var req = Transaction.FindYearTransactionStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsStatusClient.findYearlyTransactionStatusFailed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthTransactionStatusSuccessCardNumber(RoutingContext ctx) {
    var req = Transaction.FindMonthlyTransactionStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findMonthlyTransactionStatusSuccessByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransactionStatusSuccessCardNumber(RoutingContext ctx) {
    var req = Transaction.FindYearTransactionStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findYearlyTransactionStatusSuccessByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthTransactionStatusFailedCardNumber(RoutingContext ctx) {
    var req = Transaction.FindMonthlyTransactionStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setMonth(getMonthParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findMonthlyTransactionStatusFailedByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyTransactionStatusFailedCardNumber(RoutingContext ctx) {
    var req = Transaction.FindYearTransactionStatusCardNumber.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsStatusClient.findYearlyTransactionStatusFailedByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS METHOD ==
  public void getMonthlyPaymentMethods(RoutingContext ctx) {
    var req = Transaction.FindYearTransactionStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsMethodClient.findMonthlyPaymentMethods(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyPaymentMethods(RoutingContext ctx) {
    var req = Transaction.FindYearTransactionStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsMethodClient.findYearlyPaymentMethods(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyPaymentMethodsByCardNumber(RoutingContext ctx) {
    var req = Transaction.FindByYearCardNumberTransactionRequest.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsMethodClient.findMonthlyPaymentMethodsByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyPaymentMethodsByCardNumber(RoutingContext ctx) {
    var req = Transaction.FindByYearCardNumberTransactionRequest.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsMethodClient.findYearlyPaymentMethodsByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // == STATS AMOUNT ==
  public void getMonthlyAmounts(RoutingContext ctx) {
    var req = Transaction.FindYearTransactionStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsAmountClient.findMonthlyAmounts(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyAmounts(RoutingContext ctx) {
    var req = Transaction.FindYearTransactionStatus.newBuilder().setYear(getYearParam(ctx)).build();
    statsAmountClient.findYearlyAmounts(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyAmountsByCardNumber(RoutingContext ctx) {
    var req = Transaction.FindByYearCardNumberTransactionRequest.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsAmountClient.findMonthlyAmountsByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyAmountsByCardNumber(RoutingContext ctx) {
    var req = Transaction.FindByYearCardNumberTransactionRequest.newBuilder().setYear(getYearParam(ctx)).setCardNumber(ctx.pathParam("cardNumber")).build();
    statsAmountClient.findYearlyAmountsByCardNumber(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  private void sendResponse(RoutingContext ctx, com.google.protobuf.MessageOrBuilder proto, int defaultStatus) {
    JsonObject json = ProtoMapper.toJson(proto);
    int status = json.getInteger("status", defaultStatus);
    ctx.response()
        .setStatusCode(status == 0 ? defaultStatus : status)
        .putHeader("Content-Type", "application/json")
        .end(json.encode());
  }
}
