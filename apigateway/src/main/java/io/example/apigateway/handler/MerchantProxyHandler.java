package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.merchant.Merchant;
import pb.merchant.MerchantCommand;
import pb.merchant.VertxMerchantCommandServiceGrpcClient;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;
import pb.merchant.VertxMerchantTransactionServiceGrpcClient;
import pb.merchant.stats.VertxMerchantStatsAmountServiceGrpcClient;
import pb.merchant.stats.VertxMerchantStatsMethodServiceGrpcClient;
import pb.merchant.stats.VertxMerchantStatsTotalAmountServiceGrpcClient;
import pb.merchant_document.MerchantDocumentCommand;
import pb.merchant_document.MerchantDocumentOuterClass;
import pb.merchant_document.VertxMerchantDocumentCommandServiceGrpcClient;
import pb.merchant_document.VertxMerchantDocumentQueryServiceGrpcClient;

public class MerchantProxyHandler {
  private final VertxMerchantQueryServiceGrpcClient queryClient;
  private final VertxMerchantCommandServiceGrpcClient commandClient;
  private final VertxMerchantDocumentCommandServiceGrpcClient docCommandClient;
  private final VertxMerchantDocumentQueryServiceGrpcClient docQueryClient;
  private final VertxMerchantStatsAmountServiceGrpcClient statsAmountClient;
  private final VertxMerchantStatsMethodServiceGrpcClient statsMethodClient;
  private final VertxMerchantStatsTotalAmountServiceGrpcClient statsTotalAmountClient;
  private final VertxMerchantTransactionServiceGrpcClient txnClient;

  public MerchantProxyHandler(
      VertxMerchantQueryServiceGrpcClient queryClient,
      VertxMerchantCommandServiceGrpcClient commandClient,
      VertxMerchantDocumentCommandServiceGrpcClient docCommandClient,
      VertxMerchantDocumentQueryServiceGrpcClient docQueryClient,
      VertxMerchantStatsAmountServiceGrpcClient statsAmountClient,
      VertxMerchantStatsMethodServiceGrpcClient statsMethodClient,
      VertxMerchantStatsTotalAmountServiceGrpcClient statsTotalAmountClient,
      VertxMerchantTransactionServiceGrpcClient txnClient) {
    this.queryClient = queryClient;
    this.commandClient = commandClient;
    this.docCommandClient = docCommandClient;
    this.docQueryClient = docQueryClient;
    this.statsAmountClient = statsAmountClient;
    this.statsMethodClient = statsMethodClient;
    this.statsTotalAmountClient = statsTotalAmountClient;
    this.txnClient = txnClient;
  }

  private int getYearParam(RoutingContext ctx) {
    return ctx.queryParams().contains("year") ? Integer.parseInt(ctx.queryParams().get("year")) : 2024;
  }

  // =========================================================================
  // MERCHANTS QUERIES
  // =========================================================================

  public void getAllMerchants(RoutingContext ctx) {
    var req = Merchant.FindAllMerchantRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findAllMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getActiveMerchants(RoutingContext ctx) {
    var req = Merchant.FindAllMerchantRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByActive(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTrashedMerchants(RoutingContext ctx) {
    var req = Merchant.FindAllMerchantRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    queryClient.findByTrashed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMerchantById(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("merchantId"));
    var req = Merchant.FindByIdMerchantRequest.newBuilder().setMerchantId(id).build();
    queryClient.findByIdMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMerchantByApiKey(RoutingContext ctx) {
    String key = ctx.pathParam("apiKey");
    var req = Merchant.FindByApiKeyRequest.newBuilder().setApiKey(key).build();
    queryClient.findByApiKey(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMerchantByName(RoutingContext ctx) {
    // Not provided by grpc, return empty array or use queryClient to query all.
    ctx.response().setStatusCode(200).putHeader("Content-Type", "application/json")
        .end("{\"status\":\"200\",\"message\":\"success\",\"data\":[]}");
  }

  public void getMerchantsByUserId(RoutingContext ctx) {
    int uId = Integer.parseInt(ctx.pathParam("userId"));
    var req = Merchant.FindByMerchantUserIdRequest.newBuilder().setUserId(uId).build();
    queryClient.findByMerchantUserId(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // =========================================================================
  // MERCHANTS COMMANDS
  // =========================================================================

  public void createMerchant(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = MerchantCommand.CreateMerchantRequest.newBuilder()
        .setUserId(body.getInteger("user_id", 0))
        .setName(body.getString("name", ""))
        .build();
    commandClient.createMerchant(req).onSuccess(r -> sendResponse(ctx, r, 201)).onFailure(ctx::fail);
  }

  public void updateMerchant(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = MerchantCommand.UpdateMerchantRequest.newBuilder()
        .setMerchantId(body.getInteger("id", 0))
        .setUserId(body.getInteger("user_id", 0))
        .setName(body.getString("name", ""))
        .setStatus(body.getString("status", ""))
        .build();
    commandClient.updateMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void updateMerchantStatus(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = MerchantCommand.UpdateMerchantStatusRequest.newBuilder()
        .setMerchantId(body.getInteger("id", 0))
        .setStatus(body.getString("status", ""))
        .build();
    commandClient.updateMerchantStatus(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void trashMerchant(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("merchantId"));
    var req = Merchant.FindByIdMerchantRequest.newBuilder().setMerchantId(id).build();
    commandClient.trashedMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreMerchant(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("merchantId"));
    var req = Merchant.FindByIdMerchantRequest.newBuilder().setMerchantId(id).build();
    commandClient.restoreMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteMerchantPermanently(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("merchantId"));
    var req = Merchant.FindByIdMerchantRequest.newBuilder().setMerchantId(id).build();
    commandClient.deleteMerchantPermanent(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreAllMerchants(RoutingContext ctx) {
    commandClient.restoreAllMerchant(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteAllPermanentMerchants(RoutingContext ctx) {
    commandClient.deleteAllMerchantPermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // =========================================================================
  // ANALYTICS - TRANSACTIONAL & METHODS
  // =========================================================================

  public void findAllTransactions(RoutingContext ctx) {
    var req = Merchant.FindAllMerchantTransaction.newBuilder()
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .build();
    txnClient.findAllTransactionMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void findAllTransactionsByApiKey(RoutingContext ctx) {
    String key = ctx.pathParam("apiKey");
    var req = Merchant.FindAllMerchantTransactionApikey.newBuilder()
        .setApiKey(key)
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .build();
    txnClient.findAllTransactionByApikey(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void findAllTransactionsByMerchantId(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("merchantId"));
    var req = Merchant.FindAllMerchantTransactionId.newBuilder()
        .setId(String.valueOf(id))
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .build();
    txnClient.findAllTransactionByMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  // === GLOBAL STATS ===
  public void getMonthlyPaymentMethodsMerchant(RoutingContext ctx) {
    var req = Merchant.FindYearMerchant.newBuilder().setYear(getYearParam(ctx)).build();
    statsMethodClient.findMonthlyPaymentMethodsMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyPaymentMethodMerchant(RoutingContext ctx) {
    var req = Merchant.FindYearMerchant.newBuilder().setYear(getYearParam(ctx)).build();
    statsMethodClient.findYearlyPaymentMethodMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getMonthlyAmountMerchant(RoutingContext ctx) {
    var req = Merchant.FindYearMerchant.newBuilder().setYear(getYearParam(ctx)).build();
    statsAmountClient.findMonthlyAmountMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyAmountMerchant(RoutingContext ctx) {
    var req = Merchant.FindYearMerchant.newBuilder().setYear(getYearParam(ctx)).build();
    statsAmountClient.findYearlyAmountMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyTotalAmountMerchant(RoutingContext ctx) {
    var req = Merchant.FindYearMerchant.newBuilder().setYear(getYearParam(ctx)).build();
    statsTotalAmountClient.findMonthlyTotalAmountMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyTotalAmountMerchant(RoutingContext ctx) {
    var req = Merchant.FindYearMerchant.newBuilder().setYear(getYearParam(ctx)).build();
    statsTotalAmountClient.findYearlyTotalAmountMerchant(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  // === STATS BY ID ===
  public void getMonthlyPaymentMethodByMerchant(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("merchantId"));
    var req = Merchant.FindYearMerchantById.newBuilder().setYear(getYearParam(ctx)).setMerchantId(id).build();
    statsMethodClient.findMonthlyPaymentMethodByMerchants(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyPaymentMethodByMerchants(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("merchantId"));
    var req = Merchant.FindYearMerchantById.newBuilder().setYear(getYearParam(ctx)).setMerchantId(id).build();
    statsMethodClient.findYearlyPaymentMethodByMerchants(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getMonthlyAmountByMerchants(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("merchantId"));
    var req = Merchant.FindYearMerchantById.newBuilder().setYear(getYearParam(ctx)).setMerchantId(id).build();
    statsAmountClient.findMonthlyAmountByMerchants(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyAmountByMerchants(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("merchantId"));
    var req = Merchant.FindYearMerchantById.newBuilder().setYear(getYearParam(ctx)).setMerchantId(id).build();
    statsAmountClient.findYearlyAmountByMerchants(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyTotalAmountByMerchant(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("merchantId"));
    var req = Merchant.FindYearMerchantById.newBuilder().setYear(getYearParam(ctx)).setMerchantId(id).build();
    statsTotalAmountClient.findMonthlyTotalAmountByMerchants(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyTotalAmountByMerchant(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("merchantId"));
    var req = Merchant.FindYearMerchantById.newBuilder().setYear(getYearParam(ctx)).setMerchantId(id).build();
    statsTotalAmountClient.findYearlyTotalAmountByMerchants(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  // === STATS BY API KEY ===
  public void getMonthlyPaymentMethodByApiKey(RoutingContext ctx) {
    String key = ctx.pathParam("apiKey");
    var req = Merchant.FindYearMerchantByApikey.newBuilder().setYear(getYearParam(ctx)).setApiKey(key).build();
    statsMethodClient.findMonthlyPaymentMethodByApikey(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyPaymentMethodByApiKey(RoutingContext ctx) {
    String key = ctx.pathParam("apiKey");
    var req = Merchant.FindYearMerchantByApikey.newBuilder().setYear(getYearParam(ctx)).setApiKey(key).build();
    statsMethodClient.findYearlyPaymentMethodByApikey(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getMonthlyAmountByApiKey(RoutingContext ctx) {
    String key = ctx.pathParam("apiKey");
    var req = Merchant.FindYearMerchantByApikey.newBuilder().setYear(getYearParam(ctx)).setApiKey(key).build();
    statsAmountClient.findMonthlyAmountByApikey(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getYearlyAmountByApiKey(RoutingContext ctx) {
    String key = ctx.pathParam("apiKey");
    var req = Merchant.FindYearMerchantByApikey.newBuilder().setYear(getYearParam(ctx)).setApiKey(key).build();
    statsAmountClient.findYearlyAmountByApikey(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMonthlyTotalAmountByApiKey(RoutingContext ctx) {
    String key = ctx.pathParam("apiKey");
    var req = Merchant.FindYearMerchantByApikey.newBuilder().setYear(getYearParam(ctx)).setApiKey(key).build();
    statsTotalAmountClient.findMonthlyTotalAmountByApikey(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  public void getYearlyTotalAmountByApiKey(RoutingContext ctx) {
    String key = ctx.pathParam("apiKey");
    var req = Merchant.FindYearMerchantByApikey.newBuilder().setYear(getYearParam(ctx)).setApiKey(key).build();
    statsTotalAmountClient.findYearlyTotalAmountByApikey(req).onSuccess(r -> sendResponse(ctx, r, 200))
        .onFailure(ctx::fail);
  }

  // =========================================================================
  // MERCHANT DOCUMENTS
  // =========================================================================

  public void getAllMerchantDocuments(RoutingContext ctx) {
    var req = MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    docQueryClient.findAll(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getActiveMerchantDocuments(RoutingContext ctx) {
    var req = MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    docQueryClient.findAllActive(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getTrashedMerchantDocuments(RoutingContext ctx) {
    var req = MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
        .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
        .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
        .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
        .build();
    docQueryClient.findAllTrashed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void getMerchantDocumentById(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("documentId"));
    var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder().setDocumentId(id).build();
    docQueryClient.findById(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void createMerchantDocument(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    var req = MerchantDocumentCommand.CreateMerchantDocumentRequest.newBuilder()
        .setMerchantId(body.getInteger("merchant_id", 0))
        .setDocumentType(body.getString("document_type", ""))
        .setDocumentUrl(body.getString("document_path", ""))
        .build();
    docCommandClient.create(req).onSuccess(r -> sendResponse(ctx, r, 201)).onFailure(ctx::fail);
  }

  public void updateMerchantDocument(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    int id = Integer.parseInt(ctx.pathParam("documentId"));
    var req = MerchantDocumentCommand.UpdateMerchantDocumentRequest.newBuilder()
        .setDocumentId(id)
        .setMerchantId(body.getInteger("merchant_id", 0))
        .setDocumentType(body.getString("document_type", ""))
        .setDocumentUrl(body.getString("document_path", ""))
        .build();
    docCommandClient.update(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void updateMerchantDocumentStatus(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    int id = Integer.parseInt(ctx.pathParam("documentId"));
    var req = MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest.newBuilder()
        .setDocumentId(id)
        .setStatus(body.getString("status", ""))
        .setNote(body.getString("note", ""))
        .build();
    docCommandClient.updateStatus(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void trashMerchantDocument(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("documentId"));
    var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder().setDocumentId(id).build();
    docCommandClient.trashed(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreMerchantDocument(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("documentId"));
    var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder().setDocumentId(id).build();
    docCommandClient.restore(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteMerchantDocumentPermanently(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("documentId"));
    var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder().setDocumentId(id).build();
    docCommandClient.deletePermanent(req).onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void restoreAllMerchantDocuments(RoutingContext ctx) {
    docCommandClient.restoreAll(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
  }

  public void deleteAllPermanentMerchantDocuments(RoutingContext ctx) {
    docCommandClient.deleteAllPermanent(com.google.protobuf.Empty.getDefaultInstance())
        .onSuccess(r -> sendResponse(ctx, r, 200)).onFailure(ctx::fail);
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
