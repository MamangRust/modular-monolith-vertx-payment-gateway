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
import pb.card.Card;
import pb.card.CardAuthorization;
import pb.card.CardBilling;
import pb.card.CardCommand;
import pb.card.CardDashboard;
import pb.card.CardLimit;
import pb.card.CardPayment;
import pb.card.VertxCardAuthorizationServiceGrpcClient;
import pb.card.VertxCardBillingServiceGrpcClient;
import pb.card.VertxCardCommandServiceGrpcClient;
import pb.card.VertxCardDashboardServiceGrpcClient;
import pb.card.VertxCardLimitServiceGrpcClient;
import pb.card.VertxCardPaymentServiceGrpcClient;
import pb.card.VertxCardQueryServiceGrpcClient;
import pb.card.stats.VertxCardStatsBalanceServiceGrpcClient;
import pb.card.stats.VertxCardStatsTopupServiceGrpcClient;
import pb.card.stats.VertxCardStatsTransactionServiceGrpcClient;
import pb.card.stats.VertxCardStatsTransferServiceGrpcClient;
import pb.card.stats.VertxCardStatsWithdrawServiceGrpcClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardProxyHandlerTest {

  @Mock private VertxCardQueryServiceGrpcClient queryClient;
  @Mock private VertxCardCommandServiceGrpcClient commandClient;
  @Mock private VertxCardDashboardServiceGrpcClient dashboardClient;
  @Mock private VertxCardStatsBalanceServiceGrpcClient balanceClient;
  @Mock private VertxCardStatsTopupServiceGrpcClient topupClient;
  @Mock private VertxCardStatsWithdrawServiceGrpcClient withdrawClient;
  @Mock private VertxCardStatsTransactionServiceGrpcClient transactionClient;
  @Mock private VertxCardStatsTransferServiceGrpcClient transferClient;
  @Mock private VertxCardAuthorizationServiceGrpcClient authClient;
  @Mock private VertxCardPaymentServiceGrpcClient paymentClient;
  @Mock private VertxCardBillingServiceGrpcClient billingClient;
  @Mock private VertxCardLimitServiceGrpcClient limitClient;

  @Mock private RoutingContext ctx;
  @Mock private RequestBody reqBody;
  @Mock private io.vertx.core.http.HttpServerResponse response;

  private CardProxyHandler handler;
  private MockedStatic<GrpcGatewayUtils> utils;

  @BeforeEach
  void setUp() {
    handler = new CardProxyHandler(
        queryClient, commandClient, dashboardClient, balanceClient, topupClient, withdrawClient,
        transactionClient, transferClient, authClient, paymentClient, billingClient, limitClient
    );
    utils = mockStatic(GrpcGatewayUtils.class);
  }

  @AfterEach
  void tearDown() {
    utils.close();
  }

  @Test
  void getAllCards() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);
    when(queryClient.findAllCard(any(Card.FindAllCardRequest.class)))
        .thenReturn(Future.succeededFuture(pb.card.CardQuery.ApiResponsePaginationCard.getDefaultInstance()));

    handler.getAllCards(ctx);
    verify(queryClient).findAllCard(any(Card.FindAllCardRequest.class));
  }

  @Test
  void createCard() {
    var body = new JsonObject().put("user_id", 1).put("card_type", "CREDIT");
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("user_id"), anyInt())).thenReturn(1);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), anyString(), anyString())).thenReturn("val");

    when(commandClient.createCard(any(CardCommand.CreateCardRequest.class)))
        .thenReturn(Future.succeededFuture(Card.ApiResponseCard.getDefaultInstance()));

    handler.createCard(ctx);
    verify(commandClient).createCard(any(CardCommand.CreateCardRequest.class));
  }

  @Test
  void getDashboard() {
    when(dashboardClient.dashboardCard(any(com.google.protobuf.Empty.class)))
        .thenReturn(Future.succeededFuture(CardDashboard.ApiResponseDashboardCard.getDefaultInstance()));

    handler.getDashboard(ctx);
    verify(dashboardClient).dashboardCard(any(com.google.protobuf.Empty.class));
  }

  @Test
  void handleAuthorize() {
    var body = new JsonObject().put("card_number", "1234").put("amount", 1000L);
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("card_number"), anyString())).thenReturn("1234");
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("merchant_id"), anyInt())).thenReturn(1);
    utils.when(() -> GrpcGatewayUtils.getJsonLong(eq(body), eq("amount"), anyLong())).thenReturn(1000L);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("currency"), anyString())).thenReturn("IDR");
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("pos_entry_mode"), anyString())).thenReturn("01");
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("mcc"), anyString())).thenReturn("5000");
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("idempotency_key"), anyString())).thenReturn("key");

    when(authClient.authorize(any(CardAuthorization.AuthorizeRequest.class)))
        .thenReturn(Future.succeededFuture(CardAuthorization.AuthorizeResponse.getDefaultInstance()));

    handler.handleAuthorize(ctx);
    verify(authClient).authorize(any(CardAuthorization.AuthorizeRequest.class));
  }

  @Test
  void handleReversal() {
    var body = new JsonObject().put("txn_id", "t1").put("amount", 1000L);
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("txn_id"), anyString())).thenReturn("t1");
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("card_number"), anyString())).thenReturn("1234");
    utils.when(() -> GrpcGatewayUtils.getJsonLong(eq(body), eq("amount"), anyLong())).thenReturn(1000L);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("idempotency_key"), anyString())).thenReturn("key");

    when(authClient.reverse(any(CardAuthorization.ReverseRequest.class)))
        .thenReturn(Future.succeededFuture(CardAuthorization.ReverseResponse.getDefaultInstance()));

    handler.handleReversal(ctx);
    verify(authClient).reverse(any(CardAuthorization.ReverseRequest.class));
  }

  @Test
  void handlePostPayment() {
    var body = new JsonObject().put("card_number", "1234").put("amount", 500L);
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("reference_id"), anyString())).thenReturn("r1");
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("card_number"), anyString())).thenReturn("1234");
    utils.when(() -> GrpcGatewayUtils.getJsonLong(eq(body), eq("amount"), anyLong())).thenReturn(500L);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("payment_channel"), anyString())).thenReturn("web");
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("statement_id"), anyInt())).thenReturn(1);

    when(paymentClient.postPayment(any(CardPayment.PostPaymentRequest.class)))
        .thenReturn(Future.succeededFuture(CardPayment.PostPaymentResponse.getDefaultInstance()));

    handler.handlePostPayment(ctx);
    verify(paymentClient).postPayment(any(CardPayment.PostPaymentRequest.class));
  }

  @Test
  void handleGetStatement() {
    when(ctx.pathParam("cardNumber")).thenReturn("1234");
    utils.when(() -> GrpcGatewayUtils.getQueryString(eq(ctx), eq("statement_date"), anyString())).thenReturn("2026-06");

    when(billingClient.getStatement(any(CardBilling.GetStatementRequest.class)))
        .thenReturn(Future.succeededFuture(CardBilling.GetStatementResponse.getDefaultInstance()));

    handler.handleGetStatement(ctx);
    verify(billingClient).getStatement(any(CardBilling.GetStatementRequest.class));
  }

  @Test
  void handleSetLimit() {
    when(ctx.pathParam("cardNumber")).thenReturn("1234");
    var body = new JsonObject().put("credit_limit", 5000L);
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonLong(eq(body), eq("credit_limit"), anyLong())).thenReturn(5000L);
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("billing_cycle_day"), anyInt())).thenReturn(5);
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("annual_rate_bps"), anyInt())).thenReturn(1800);

    when(limitClient.setLimit(any(CardLimit.SetLimitRequest.class)))
        .thenReturn(Future.succeededFuture(CardLimit.SetLimitResponse.getDefaultInstance()));

    handler.handleSetLimit(ctx);
    verify(limitClient).setLimit(any(CardLimit.SetLimitRequest.class));
  }

  @Test
  void handleGetRewards() {
    when(ctx.response()).thenReturn(response);
    when(response.putHeader(anyString(), anyString())).thenReturn(response);

    handler.handleGetRewards(ctx);
    verify(response).end(anyString());
  }
}
