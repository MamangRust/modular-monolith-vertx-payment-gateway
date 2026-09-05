package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.transaction.Transaction;
import pb.transaction.TransactionCommand;
import pb.transaction.TransactionQuery;
import pb.transaction.VertxTransactionCommandServiceGrpcClient;
import pb.transaction.VertxTransactionQueryServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsAmountServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsMethodServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsStatusServiceGrpcClient;

import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionProxyHandlerTest {

  @Mock private VertxTransactionQueryServiceGrpcClient queryClient;
  @Mock private VertxTransactionCommandServiceGrpcClient commandClient;
  @Mock private VertxTransactionStatsAmountServiceGrpcClient statsAmountClient;
  @Mock private VertxTransactionStatsMethodServiceGrpcClient statsMethodClient;
  @Mock private VertxTransactionStatsStatusServiceGrpcClient statsStatusClient;

  @Mock private RoutingContext ctx;
  @Mock private RequestBody reqBody;

  private TransactionProxyHandler handler;
  private MockedStatic<GrpcGatewayUtils> utils;

  @BeforeEach
  void setUp() {
    handler = new TransactionProxyHandler(queryClient, commandClient, statsAmountClient, statsMethodClient, statsStatusClient);
    utils = mockStatic(GrpcGatewayUtils.class);
  }

  @AfterEach
  void tearDown() {
    utils.close();
  }

  @Test
  void getTransactions() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);
    when(queryClient.findAllTransaction(any(TransactionQuery.FindAllTransactionRequest.class)))
        .thenReturn(Future.succeededFuture(TransactionQuery.ApiResponsePaginationTransaction.getDefaultInstance()));

    handler.getTransactions(ctx);
    verify(queryClient).findAllTransaction(any(TransactionQuery.FindAllTransactionRequest.class));
  }

  @Test
  void getTransactionById() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "transactionId")).thenReturn(5);
    when(queryClient.findByIdTransaction(any(Transaction.FindByIdTransactionRequest.class)))
        .thenReturn(Future.succeededFuture(Transaction.ApiResponseTransaction.getDefaultInstance()));

    handler.getTransactionById(ctx);
    verify(queryClient).findByIdTransaction(any(Transaction.FindByIdTransactionRequest.class));
  }

  @Test
  void createTransaction() {
    var body = new JsonObject().put("card_number", "1234").put("amount", 200)
        .put("idempotency_key", "txn-idem-gateway");
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("card_number"), anyString())).thenReturn("1234");
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("amount"), anyInt())).thenReturn(200);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("payment_method"), anyString())).thenReturn("QRIS");
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("idempotency_key"), anyString()))
        .thenReturn("txn-idem-gateway");

    pb.merchant.Merchant.MerchantResponse merchant = pb.merchant.Merchant.MerchantResponse.newBuilder()
        .setId(88)
        .build();
    when(ctx.get("merchant")).thenReturn(merchant);
    // ApiKeyMiddleware stores the validated key in the context; the handler
    // forwards it as-is, so the test must provide one to avoid setApiKey(null).
    when(ctx.get("apiKey")).thenReturn("merchant-api-key");

    when(commandClient.createTransaction(any(TransactionCommand.CreateTransactionRequest.class)))
        .thenReturn(Future.succeededFuture(Transaction.ApiResponseTransaction.getDefaultInstance()));

    handler.createTransaction(ctx);
    var requestCaptor = ArgumentCaptor.forClass(TransactionCommand.CreateTransactionRequest.class);
    verify(commandClient).createTransaction(requestCaptor.capture());
    org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().getIdempotencyKey())
        .isEqualTo("txn-idem-gateway");
  }

  @Test
  void getMonthTransactionStatusSuccess() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("month"), anyInt())).thenReturn(1);

    when(statsStatusClient.findMonthlyTransactionStatusSuccess(any(Transaction.FindMonthlyTransactionStatus.class)))
        .thenReturn(Future.succeededFuture(pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess.getDefaultInstance()));

    handler.getMonthTransactionStatusSuccess(ctx);
    verify(statsStatusClient).findMonthlyTransactionStatusSuccess(any(Transaction.FindMonthlyTransactionStatus.class));
  }

  @Test
  void getMonthlyPaymentMethods() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);

    when(statsMethodClient.findMonthlyPaymentMethods(any(Transaction.FindYearTransactionStatus.class)))
        .thenReturn(Future.succeededFuture(pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionMonthMethod.getDefaultInstance()));

    handler.getMonthlyPaymentMethods(ctx);
    verify(statsMethodClient).findMonthlyPaymentMethods(any(Transaction.FindYearTransactionStatus.class));
  }
}
