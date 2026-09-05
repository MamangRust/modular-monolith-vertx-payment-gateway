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
import pb.merchant.Merchant;
import pb.merchant.MerchantCommand;
import pb.merchant.MerchantQuery;
import pb.merchant.MerchantTransaction;
import pb.merchant.VertxMerchantCommandServiceGrpcClient;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;
import pb.merchant.VertxMerchantTransactionServiceGrpcClient;
import pb.merchant.stats.VertxMerchantStatsAmountServiceGrpcClient;
import pb.merchant.stats.VertxMerchantStatsMethodServiceGrpcClient;
import pb.merchant.stats.VertxMerchantStatsTotalAmountServiceGrpcClient;
import pb.merchant_document.MerchantDocumentCommand;
import pb.merchant_document.MerchantDocumentOuterClass;
import pb.merchant_document.MerchantDocumentQuery;
import pb.merchant_document.VertxMerchantDocumentCommandServiceGrpcClient;
import pb.merchant_document.VertxMerchantDocumentQueryServiceGrpcClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantProxyHandlerTest {

  @Mock private VertxMerchantQueryServiceGrpcClient queryClient;
  @Mock private VertxMerchantCommandServiceGrpcClient commandClient;
  @Mock private VertxMerchantDocumentCommandServiceGrpcClient docCommandClient;
  @Mock private VertxMerchantDocumentQueryServiceGrpcClient docQueryClient;
  @Mock private VertxMerchantStatsAmountServiceGrpcClient statsAmountClient;
  @Mock private VertxMerchantStatsMethodServiceGrpcClient statsMethodClient;
  @Mock private VertxMerchantStatsTotalAmountServiceGrpcClient statsTotalAmountClient;
  @Mock private VertxMerchantTransactionServiceGrpcClient txnClient;

  @Mock private RoutingContext ctx;
  @Mock private RequestBody reqBody;
  @Mock private io.vertx.core.http.HttpServerResponse response;

  private MerchantProxyHandler handler;
  private MockedStatic<GrpcGatewayUtils> utils;

  @BeforeEach
  void setUp() {
    handler = new MerchantProxyHandler(
        queryClient, commandClient, docCommandClient, docQueryClient,
        statsAmountClient, statsMethodClient, statsTotalAmountClient, txnClient
    );
    utils = mockStatic(GrpcGatewayUtils.class);
  }

  @AfterEach
  void tearDown() {
    utils.close();
  }

  @Test
  void getAllMerchants() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);
    when(queryClient.findAllMerchant(any(Merchant.FindAllMerchantRequest.class)))
        .thenReturn(Future.succeededFuture(MerchantQuery.ApiResponsePaginationMerchant.getDefaultInstance()));

    handler.getAllMerchants(ctx);
    verify(queryClient).findAllMerchant(any(Merchant.FindAllMerchantRequest.class));
  }

  @Test
  void getMerchantByApiKey() {
    when(ctx.pathParam("apiKey")).thenReturn("apikey123");
    when(queryClient.findByApiKey(any(Merchant.FindByApiKeyRequest.class)))
        .thenReturn(Future.succeededFuture(Merchant.ApiResponseMerchant.getDefaultInstance()));

    handler.getMerchantByApiKey(ctx);
    verify(queryClient).findByApiKey(any(Merchant.FindByApiKeyRequest.class));
  }

  @Test
  void createMerchant() {
    var body = new JsonObject().put("user_id", 1).put("name", "Shop");
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("user_id"), anyInt())).thenReturn(1);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), eq("name"), anyString())).thenReturn("Shop");

    when(commandClient.createMerchant(any(MerchantCommand.CreateMerchantRequest.class)))
        .thenReturn(Future.succeededFuture(Merchant.ApiResponseMerchant.getDefaultInstance()));

    handler.createMerchant(ctx);
    verify(commandClient).createMerchant(any(MerchantCommand.CreateMerchantRequest.class));
  }

  @Test
  void getMerchantByName() {
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(200)).thenReturn(response);
    when(response.putHeader(anyString(), anyString())).thenReturn(response);

    handler.getMerchantByName(ctx);
    verify(response).end(anyString());
  }

  @Test
  void findAllTransactionsByApiKey() {
    when(ctx.pathParam("apiKey")).thenReturn("key");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("search");

    when(txnClient.findAllTransactionByApikey(any(Merchant.FindAllMerchantTransactionApikey.class)))
        .thenReturn(Future.succeededFuture(MerchantTransaction.ApiResponsePaginationMerchantTransaction.getDefaultInstance()));

    handler.findAllTransactionsByApiKey(ctx);
    verify(txnClient).findAllTransactionByApikey(any(Merchant.FindAllMerchantTransactionApikey.class));
  }

  @Test
  void getMonthlyPaymentMethodsMerchant() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);
    when(statsMethodClient.findMonthlyPaymentMethodsMerchant(any(Merchant.FindYearMerchant.class)))
        .thenReturn(Future.succeededFuture(pb.merchant.stats.MerchantStatsMethod.ApiResponseMerchantMonthlyPaymentMethod.getDefaultInstance()));

    handler.getMonthlyPaymentMethodsMerchant(ctx);
    verify(statsMethodClient).findMonthlyPaymentMethodsMerchant(any(Merchant.FindYearMerchant.class));
  }

  @Test
  void getMonthlyPaymentMethodByMerchant() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);
    utils.when(() -> GrpcGatewayUtils.getSafePathInt(ctx, "merchantId")).thenReturn(5);

    when(statsMethodClient.findMonthlyPaymentMethodByMerchants(any(Merchant.FindYearMerchantById.class)))
        .thenReturn(Future.succeededFuture(pb.merchant.stats.MerchantStatsMethod.ApiResponseMerchantMonthlyPaymentMethod.getDefaultInstance()));

    handler.getMonthlyPaymentMethodByMerchant(ctx);
    verify(statsMethodClient).findMonthlyPaymentMethodByMerchants(any(Merchant.FindYearMerchantById.class));
  }

  @Test
  void getMonthlyPaymentMethodByApiKey() {
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), eq("year"), anyInt())).thenReturn(2024);
    when(ctx.pathParam("apiKey")).thenReturn("key");

    when(statsMethodClient.findMonthlyPaymentMethodByApikey(any(Merchant.FindYearMerchantByApikey.class)))
        .thenReturn(Future.succeededFuture(pb.merchant.stats.MerchantStatsMethod.ApiResponseMerchantMonthlyPaymentMethod.getDefaultInstance()));

    handler.getMonthlyPaymentMethodByApiKey(ctx);
    verify(statsMethodClient).findMonthlyPaymentMethodByApikey(any(Merchant.FindYearMerchantByApikey.class));
  }

  @Test
  void getActiveMerchantDocuments() {
    utils.when(() -> GrpcGatewayUtils.getQueryString(any(), anyString(), anyString())).thenReturn("");
    utils.when(() -> GrpcGatewayUtils.getQueryInt(any(), anyString(), anyInt())).thenReturn(1);

    when(docQueryClient.findAllActive(any(MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.class)))
        .thenReturn(Future.succeededFuture(MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt.getDefaultInstance()));

    handler.getActiveMerchantDocuments(ctx);
    verify(docQueryClient).findAllActive(any(MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.class));
  }

  @Test
  void createMerchantDocument() {
    var body = new JsonObject().put("merchant_id", 1).put("document_type", "NIB").put("document_path", "http://path");
    when(ctx.body()).thenReturn(reqBody);
    when(reqBody.asJsonObject()).thenReturn(body);
    utils.when(() -> GrpcGatewayUtils.getJsonInteger(eq(body), eq("merchant_id"), anyInt())).thenReturn(1);
    utils.when(() -> GrpcGatewayUtils.getJsonString(eq(body), anyString(), anyString())).thenReturn("val");

    when(docCommandClient.create(any(MerchantDocumentCommand.CreateMerchantDocumentRequest.class)))
        .thenReturn(Future.succeededFuture(MerchantDocumentOuterClass.ApiResponseMerchantDocument.getDefaultInstance()));

    handler.createMerchantDocument(ctx);
    verify(docCommandClient).create(any(MerchantDocumentCommand.CreateMerchantDocumentRequest.class));
  }
}
