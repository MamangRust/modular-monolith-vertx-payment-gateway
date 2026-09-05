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
import pb.transfer.Transfer;
import pb.transfer.TransferCommand;
import pb.transfer.TransferQuery;
import pb.transfer.VertxTransferCommandServiceGrpcClient;
import pb.transfer.VertxTransferQueryServiceGrpcClient;
import pb.transfer.stats.VertxTransferStatsAmountServiceGrpcClient;
import pb.transfer.stats.VertxTransferStatsStatusServiceGrpcClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferProxyHandlerTest {

  @Mock private VertxTransferQueryServiceGrpcClient queryClient;
  @Mock private VertxTransferCommandServiceGrpcClient commandClient;
  @Mock private VertxTransferStatsAmountServiceGrpcClient statsAmountClient;
  @Mock private VertxTransferStatsStatusServiceGrpcClient statsStatusClient;

  @Mock private RoutingContext ctx;
  @Mock private RequestBody reqBody;

  private TransferProxyHandler handler;
  private MockedStatic<GrpcGatewayUtils> utils;

  @BeforeEach
  void setUp() {
    handler = new TransferProxyHandler(queryClient, commandClient, statsAmountClient, statsStatusClient);
    utils = mockStatic(GrpcGatewayUtils.class);
  }

  @AfterEach
  void tearDown() {
    utils.close();
  }

  @Test
  void getAllTransfers() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);
    when(queryClient.findAllTransfer(any(Transfer.FindAllTransferRequest.class)))
        .thenReturn(Future.succeededFuture(TransferQuery.ApiResponsePaginationTransfer.getDefaultInstance()));

    handler.getAllTransfers(ctx);
    verify(queryClient).findAllTransfer(any(Transfer.FindAllTransferRequest.class));
  }

  @Test
  void getTransferById() {
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "transferId")).thenReturn(5);
    when(queryClient.findByIdTransfer(any(Transfer.FindByIdTransferRequest.class)))
        .thenReturn(Future.succeededFuture(Transfer.ApiResponseTransfer.getDefaultInstance()));

    handler.getTransferById(ctx);
    verify(queryClient).findByIdTransfer(any(Transfer.FindByIdTransferRequest.class));
  }

  @Test
  void getTransfersByCardNumber() {
    when(ctx.pathParam("cardNumber")).thenReturn("1234");
    when(queryClient.findTransferByTransferFrom(any(Transfer.FindTransferByTransferFromRequest.class)))
        .thenReturn(Future.succeededFuture(TransferQuery.ApiResponseTransfers.getDefaultInstance()));

    handler.getTransfersByCardNumber(ctx);
    verify(queryClient).findTransferByTransferFrom(any(Transfer.FindTransferByTransferFromRequest.class));
  }

  @Test
  void createTransfer() {
    var body = new JsonObject().put("sender_card_number", "111").put("receiver_card_number", "222").put("amount", 500);
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("sender_card_number"), anyString())).thenReturn("111");
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("receiver_card_number"), anyString())).thenReturn("222");
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("amount"), anyInt())).thenReturn(500);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("idempotency_key"), anyString()))
        .thenReturn("transfer-idem-test");

    when(commandClient.createTransfer(any(TransferCommand.CreateTransferRequest.class)))
        .thenReturn(Future.succeededFuture(Transfer.ApiResponseTransfer.getDefaultInstance()));

    handler.createTransfer(ctx);
    verify(commandClient).createTransfer(any(TransferCommand.CreateTransferRequest.class));
  }

  @Test
  void getMonthTransferStatusSuccess() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("month"), anyInt())).thenReturn(1);

    when(statsStatusClient.findMonthlyTransferStatusSuccess(any(Transfer.FindMonthlyTransferStatus.class)))
        .thenReturn(Future.succeededFuture(pb.transfer.stats.TransferStatsStatus.ApiResponseTransferMonthStatusSuccess.getDefaultInstance()));

    handler.getMonthTransferStatusSuccess(ctx);
    verify(statsStatusClient).findMonthlyTransferStatusSuccess(any(Transfer.FindMonthlyTransferStatus.class));
  }

  @Test
  void getMonthlyTransferAmountsBySenderCardNumber() {
    when(ctx.pathParam("cardNumber")).thenReturn("1234");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);

    when(statsAmountClient.findMonthlyTransferAmountsBySenderCardNumber(any(Transfer.FindByCardNumberTransferRequest.class)))
        .thenReturn(Future.succeededFuture(pb.transfer.stats.TransferStatsAmount.ApiResponseTransferMonthAmount.getDefaultInstance()));

    handler.getMonthlyTransferAmountsBySenderCardNumber(ctx);
    verify(statsAmountClient).findMonthlyTransferAmountsBySenderCardNumber(any(Transfer.FindByCardNumberTransferRequest.class));
  }
}
