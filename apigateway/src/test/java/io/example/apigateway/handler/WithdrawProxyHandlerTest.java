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
import pb.withdraw.Withdraw;
import pb.withdraw.WithdrawCommand;
import pb.withdraw.WithdrawQuery;
import pb.withdraw.VertxWithdrawCommandServiceGrpcClient;
import pb.withdraw.VertxWithdrawQueryServiceGrpcClient;
import pb.withdraw.stats.VertxWithdrawStatsAmountServiceGrpcClient;
import pb.withdraw.stats.VertxWithdrawStatsStatusServiceGrpcClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawProxyHandlerTest {

  @Mock private VertxWithdrawQueryServiceGrpcClient queryClient;
  @Mock private VertxWithdrawCommandServiceGrpcClient commandClient;
  @Mock private VertxWithdrawStatsAmountServiceGrpcClient statsAmountClient;
  @Mock private VertxWithdrawStatsStatusServiceGrpcClient statsStatusClient;

  @Mock private RoutingContext ctx;
  @Mock private RequestBody reqBody;

  private WithdrawProxyHandler handler;
  private MockedStatic<GrpcGatewayUtils> utils;

  @BeforeEach
  void setUp() {
    handler = new WithdrawProxyHandler(queryClient, commandClient, statsAmountClient, statsStatusClient);
    utils = mockStatic(GrpcGatewayUtils.class);
  }

  @AfterEach
  void tearDown() {
    utils.close();
  }

  @Test
  void getAllWithdraws() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);
    when(queryClient.findAllWithdraw(any(Withdraw.FindAllWithdrawRequest.class)))
        .thenReturn(Future.succeededFuture(WithdrawQuery.ApiResponsePaginationWithdraw.getDefaultInstance()));

    handler.getAllWithdraws(ctx);
    verify(queryClient).findAllWithdraw(any(Withdraw.FindAllWithdrawRequest.class));
  }

  @Test
  void getWithdrawById() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "withdrawId")).thenReturn(5);
    when(queryClient.findByIdWithdraw(any(Withdraw.FindByIdWithdrawRequest.class)))
        .thenReturn(Future.succeededFuture(Withdraw.ApiResponseWithdraw.getDefaultInstance()));

    handler.getWithdrawById(ctx);
    verify(queryClient).findByIdWithdraw(any(Withdraw.FindByIdWithdrawRequest.class));
  }

  @Test
  void createWithdraw() {
    var body = new JsonObject().put("card_number", "1234").put("amount", 2000);
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("card_number"), anyString())).thenReturn("1234");
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("amount"), anyInt())).thenReturn(2000);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("idempotency_key"), anyString()))
        .thenReturn("withdraw-idem-test");

    when(commandClient.createWithdraw(any(WithdrawCommand.CreateWithdrawRequest.class)))
        .thenReturn(Future.succeededFuture(Withdraw.ApiResponseWithdraw.getDefaultInstance()));

    handler.createWithdraw(ctx);
    verify(commandClient).createWithdraw(any(WithdrawCommand.CreateWithdrawRequest.class));
  }

  @Test
  void getMonthWithdrawStatusSuccess() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("month"), anyInt())).thenReturn(1);

    when(statsStatusClient.findMonthlyWithdrawStatusSuccess(any(Withdraw.FindMonthlyWithdrawStatus.class)))
        .thenReturn(Future.succeededFuture(pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawMonthStatusSuccess.getDefaultInstance()));

    handler.getMonthWithdrawStatusSuccess(ctx);
    verify(statsStatusClient).findMonthlyWithdrawStatusSuccess(any(Withdraw.FindMonthlyWithdrawStatus.class));
  }

  @Test
  void getMonthlyWithdrawsByCardNumber() {
    when(ctx.pathParam("cardNumber")).thenReturn("1234");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);

    when(statsAmountClient.findMonthlyWithdrawsByCardNumber(any(Withdraw.FindYearWithdrawCardNumber.class)))
        .thenReturn(Future.succeededFuture(pb.withdraw.stats.WithdrawStatsAmount.ApiResponseWithdrawMonthAmount.getDefaultInstance()));

    handler.getMonthlyWithdrawsByCardNumber(ctx);
    verify(statsAmountClient).findMonthlyWithdrawsByCardNumber(any(Withdraw.FindYearWithdrawCardNumber.class));
  }
}
