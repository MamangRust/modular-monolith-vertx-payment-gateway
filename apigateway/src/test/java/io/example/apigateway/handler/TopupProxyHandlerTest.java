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
import pb.topup.Topup;
import pb.topup.TopupCommand;
import pb.topup.TopupQuery;
import pb.topup.VertxTopupCommandServiceGrpcClient;
import pb.topup.VertxTopupQueryServiceGrpcClient;
import pb.topup.stats.VertxTopupStatsAmountServiceGrpcClient;
import pb.topup.stats.VertxTopupStatsMethodServiceGrpcClient;
import pb.topup.stats.VertxTopupStatsStatusServiceGrpcClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopupProxyHandlerTest {

  @Mock private VertxTopupQueryServiceGrpcClient queryClient;
  @Mock private VertxTopupCommandServiceGrpcClient commandClient;
  @Mock private VertxTopupStatsAmountServiceGrpcClient statsAmountClient;
  @Mock private VertxTopupStatsMethodServiceGrpcClient statsMethodClient;
  @Mock private VertxTopupStatsStatusServiceGrpcClient statsStatusClient;

  @Mock private RoutingContext ctx;
  @Mock private RequestBody reqBody;

  private TopupProxyHandler handler;
  private MockedStatic<GrpcGatewayUtils> utils;

  @BeforeEach
  void setUp() {
    handler = new TopupProxyHandler(queryClient, commandClient, statsAmountClient, statsMethodClient, statsStatusClient);
    utils = mockStatic(GrpcGatewayUtils.class);
  }

  @AfterEach
  void tearDown() {
    utils.close();
  }

  @Test
  void getTopups() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);
    when(queryClient.findAllTopup(any(TopupQuery.FindAllTopupRequest.class)))
        .thenReturn(Future.succeededFuture(TopupQuery.ApiResponsePaginationTopup.getDefaultInstance()));

    handler.getTopups(ctx);
    verify(queryClient).findAllTopup(any(TopupQuery.FindAllTopupRequest.class));
  }

  @Test
  void getTopupById() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "topupId")).thenReturn(5);
    when(queryClient.findByIdTopup(any(Topup.FindByIdTopupRequest.class)))
        .thenReturn(Future.succeededFuture(Topup.ApiResponseTopup.getDefaultInstance()));

    handler.getTopupById(ctx);
    verify(queryClient).findByIdTopup(any(Topup.FindByIdTopupRequest.class));
  }

  @Test
  void createTopup() {
    var body = new JsonObject().put("card_number", "1234").put("amount", 5000);
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("card_number"), anyString())).thenReturn("1234");
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("amount"), anyInt())).thenReturn(5000);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("topup_method"), anyString())).thenReturn("VA");
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("idempotency_key"), anyString()))
        .thenReturn("topup-idem-test");

    when(commandClient.createTopup(any(TopupCommand.CreateTopupRequest.class)))
        .thenReturn(Future.succeededFuture(pb.topup.Topup.ApiResponseTopup.getDefaultInstance()));

    handler.createTopup(ctx);
    verify(commandClient).createTopup(any(TopupCommand.CreateTopupRequest.class));
  }

  @Test
  void getMonthTopupStatusSuccess() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("month"), anyInt())).thenReturn(1);

    when(statsStatusClient.findMonthlyTopupStatusSuccess(any(Topup.FindMonthlyTopupStatus.class)))
        .thenReturn(Future.succeededFuture(pb.topup.stats.TopupStatsStatus.ApiResponseTopupMonthStatusSuccess.getDefaultInstance()));

    handler.getMonthTopupStatusSuccess(ctx);
    verify(statsStatusClient).findMonthlyTopupStatusSuccess(any(Topup.FindMonthlyTopupStatus.class));
  }

  @Test
  void getMonthTopupStatusSuccessCardNumber() {
    when(ctx.pathParam("cardNumber")).thenReturn("1234");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("month"), anyInt())).thenReturn(1);

    when(statsStatusClient.findMonthlyTopupStatusSuccessByCardNumber(any(Topup.FindMonthlyTopupStatusCardNumber.class)))
        .thenReturn(Future.succeededFuture(pb.topup.stats.TopupStatsStatus.ApiResponseTopupMonthStatusSuccess.getDefaultInstance()));

    handler.getMonthTopupStatusSuccessCardNumber(ctx);
    verify(statsStatusClient).findMonthlyTopupStatusSuccessByCardNumber(any(Topup.FindMonthlyTopupStatusCardNumber.class));
  }

  @Test
  void getMonthlyTopupAmounts() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);
    when(statsAmountClient.findMonthlyTopupAmounts(any(Topup.FindYearTopupStatus.class)))
        .thenReturn(Future.succeededFuture(pb.topup.stats.TopupStatsAmount.ApiResponseTopupMonthAmount.getDefaultInstance()));

    handler.getMonthlyTopupAmounts(ctx);
    verify(statsAmountClient).findMonthlyTopupAmounts(any(Topup.FindYearTopupStatus.class));
  }

  @Test
  void getMonthlyTopupMethods() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);
    when(statsMethodClient.findMonthlyTopupMethods(any(Topup.FindYearTopupStatus.class)))
        .thenReturn(Future.succeededFuture(pb.topup.stats.TopupStatsMethod.ApiResponseTopupMonthMethod.getDefaultInstance()));

    handler.getMonthlyTopupMethods(ctx);
    verify(statsMethodClient).findMonthlyTopupMethods(any(Topup.FindYearTopupStatus.class));
  }
}
