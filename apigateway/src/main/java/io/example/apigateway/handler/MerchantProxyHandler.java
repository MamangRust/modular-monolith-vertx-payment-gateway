package io.example.apigateway.handler;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public class MerchantProxyHandler {
    private final VertxMerchantQueryServiceGrpcClient queryClient;
    private final VertxMerchantCommandServiceGrpcClient commandClient;
    private final VertxMerchantDocumentCommandServiceGrpcClient docCommandClient;
    private final VertxMerchantDocumentQueryServiceGrpcClient docQueryClient;
    private final VertxMerchantStatsAmountServiceGrpcClient statsAmountClient;
    private final VertxMerchantStatsMethodServiceGrpcClient statsMethodClient;
    private final VertxMerchantStatsTotalAmountServiceGrpcClient statsTotalAmountClient;
    private final VertxMerchantTransactionServiceGrpcClient txnClient;

    // ==========================================
    // REUSABLE REQUEST BUILDERS (DRY Principle)
    // ==========================================

    private Merchant.FindAllMerchantRequest buildFindAllMerchantReq(RoutingContext ctx) {
        return Merchant.FindAllMerchantRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
    }

    private MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest buildFindAllDocReq(RoutingContext ctx) {
        return MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
    }

    // ✅ Baru: Digunakan untuk 6 method Global Stats
    private Merchant.FindYearMerchant buildFindYearMerchantReq(RoutingContext ctx) {
        return Merchant.FindYearMerchant.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .build();
    }

    // ✅ Baru: Digunakan untuk 6 method Stats By ID
    private Merchant.FindYearMerchantById buildFindYearMerchantByIdReq(RoutingContext ctx) {
        return Merchant.FindYearMerchantById.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setMerchantId(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId"))
                .build();
    }

    // ✅ Baru: Digunakan untuk 6 method Stats By Api Key
    private Merchant.FindYearMerchantByApikey buildFindYearMerchantByApikeyReq(RoutingContext ctx) {
        return Merchant.FindYearMerchantByApikey.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setApiKey(ctx.pathParam("apiKey"))
                .build();
    }

    // ==========================================
    // BASE MERCHANT QUERIES
    // ==========================================

    public void getAllMerchants(RoutingContext ctx) {
        queryClient.findAllMerchant(buildFindAllMerchantReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getActiveMerchants(RoutingContext ctx) {
        queryClient.findByActive(buildFindAllMerchantReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getTrashedMerchants(RoutingContext ctx) {
        queryClient.findByTrashed(buildFindAllMerchantReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getMerchantById(RoutingContext ctx) {
        var req = Merchant.FindByIdMerchantRequest.newBuilder()
                .setMerchantId(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId"))
                .build();
        queryClient.findByIdMerchant(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getMerchantByApiKey(RoutingContext ctx) {
        var req = Merchant.FindByApiKeyRequest.newBuilder().setApiKey(ctx.pathParam("apiKey")).build();
        queryClient.findByApiKey(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getMerchantByName(RoutingContext ctx) {
        ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("status", 200).put("message", "success")
                        .put("data", new JsonArray()).encode());
    }

    public void getMerchantsByUserId(RoutingContext ctx) {
        var req = Merchant.FindByMerchantUserIdRequest.newBuilder()
                .setUserId(GrpcGatewayUtils.getSafePathInt(ctx, "userId"))
                .build();
        queryClient.findByMerchantUserId(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    // ==========================================
    // MERCHANT COMMANDS (Null-Safe JSON Parsing)
    // ==========================================

    public void createMerchant(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = MerchantCommand.CreateMerchantRequest.newBuilder()
                .setUserId(GrpcGatewayUtils.getJsonInteger(body, "user_id", 0))
                .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
                .build();
        commandClient.createMerchant(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 201))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void updateMerchant(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = MerchantCommand.UpdateMerchantRequest.newBuilder()
                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "id", 0))
                .setUserId(GrpcGatewayUtils.getJsonInteger(body, "user_id", 0))
                .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
                .setStatus(GrpcGatewayUtils.getJsonString(body, "status", ""))
                .build();
        commandClient.updateMerchant(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void updateMerchantStatus(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = MerchantCommand.UpdateMerchantStatusRequest.newBuilder()
                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "id", 0))
                .setStatus(GrpcGatewayUtils.getJsonString(body, "status", ""))
                .build();
        commandClient.updateMerchantStatus(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void trashMerchant(RoutingContext ctx) {
        var req = Merchant.FindByIdMerchantRequest.newBuilder()
                .setMerchantId(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId"))
                .build();
        commandClient.trashedMerchant(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restoreMerchant(RoutingContext ctx) {
        var req = Merchant.FindByIdMerchantRequest.newBuilder()
                .setMerchantId(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId"))
                .build();
        commandClient.restoreMerchant(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deleteMerchantPermanently(RoutingContext ctx) {
        var req = Merchant.FindByIdMerchantRequest.newBuilder()
                .setMerchantId(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId"))
                .build();
        commandClient.deleteMerchantPermanent(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restoreAllMerchants(RoutingContext ctx) {
        commandClient.restoreAllMerchant(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deleteAllPermanentMerchants(RoutingContext ctx) {
        commandClient.deleteAllMerchantPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    // ==========================================
    // ANALYTICS - TRANSACTIONS
    // ==========================================

    public void findAllTransactions(RoutingContext ctx) {
        var req = Merchant.FindAllMerchantTransaction.newBuilder()
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .build();
        txnClient.findAllTransactionMerchant(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findAllTransactionsByApiKey(RoutingContext ctx) {
        var req = Merchant.FindAllMerchantTransactionApikey.newBuilder()
                .setApiKey(ctx.pathParam("apiKey"))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .build();
        txnClient.findAllTransactionByApikey(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findAllTransactionsByMerchantId(RoutingContext ctx) {
        var req = Merchant.FindAllMerchantTransactionId.newBuilder()
                .setId(String.valueOf(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId")))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .build();
        txnClient.findAllTransactionByMerchant(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    // ==========================================
    // ANALYTICS - GLOBAL STATS (Menggunakan Builder)
    // ==========================================

    public void getMonthlyPaymentMethodsMerchant(RoutingContext ctx) {
        statsMethodClient.findMonthlyPaymentMethodsMerchant(buildFindYearMerchantReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getYearlyPaymentMethodMerchant(RoutingContext ctx) {
        statsMethodClient.findYearlyPaymentMethodMerchant(buildFindYearMerchantReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getMonthlyAmountMerchant(RoutingContext ctx) {
        statsAmountClient.findMonthlyAmountMerchant(buildFindYearMerchantReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getYearlyAmountMerchant(RoutingContext ctx) {
        statsAmountClient.findYearlyAmountMerchant(buildFindYearMerchantReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getMonthlyTotalAmountMerchant(RoutingContext ctx) {
        statsTotalAmountClient.findMonthlyTotalAmountMerchant(buildFindYearMerchantReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getYearlyTotalAmountMerchant(RoutingContext ctx) {
        statsTotalAmountClient.findYearlyTotalAmountMerchant(buildFindYearMerchantReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    // ==========================================
    // ANALYTICS - STATS BY MERCHANT ID (Menggunakan Builder)
    // ==========================================

    public void getMonthlyPaymentMethodByMerchant(RoutingContext ctx) {
        statsMethodClient.findMonthlyPaymentMethodByMerchants(buildFindYearMerchantByIdReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getYearlyPaymentMethodByMerchants(RoutingContext ctx) {
        statsMethodClient.findYearlyPaymentMethodByMerchants(buildFindYearMerchantByIdReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getMonthlyAmountByMerchants(RoutingContext ctx) {
        statsAmountClient.findMonthlyAmountByMerchants(buildFindYearMerchantByIdReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getYearlyAmountByMerchants(RoutingContext ctx) {
        statsAmountClient.findYearlyAmountByMerchants(buildFindYearMerchantByIdReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getMonthlyTotalAmountByMerchant(RoutingContext ctx) {
        statsTotalAmountClient.findMonthlyTotalAmountByMerchants(buildFindYearMerchantByIdReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getYearlyTotalAmountByMerchant(RoutingContext ctx) {
        statsTotalAmountClient.findYearlyTotalAmountByMerchants(buildFindYearMerchantByIdReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    // ==========================================
    // ANALYTICS - STATS BY API KEY (Menggunakan Builder)
    // ==========================================

    public void getMonthlyPaymentMethodByApiKey(RoutingContext ctx) {
        statsMethodClient.findMonthlyPaymentMethodByApikey(buildFindYearMerchantByApikeyReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getYearlyPaymentMethodByApiKey(RoutingContext ctx) {
        statsMethodClient.findYearlyPaymentMethodByApikey(buildFindYearMerchantByApikeyReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getMonthlyAmountByApiKey(RoutingContext ctx) {
        statsAmountClient.findMonthlyAmountByApikey(buildFindYearMerchantByApikeyReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getYearlyAmountByApiKey(RoutingContext ctx) {
        statsAmountClient.findYearlyAmountByApikey(buildFindYearMerchantByApikeyReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getMonthlyTotalAmountByApiKey(RoutingContext ctx) {
        statsTotalAmountClient.findMonthlyTotalAmountByApikey(buildFindYearMerchantByApikeyReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getYearlyTotalAmountByApiKey(RoutingContext ctx) {
        statsTotalAmountClient.findYearlyTotalAmountByApikey(buildFindYearMerchantByApikeyReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    // ==========================================
    // MERCHANT DOCUMENTS
    // ==========================================

    public void getAllMerchantDocuments(RoutingContext ctx) {
        docQueryClient.findAll(buildFindAllDocReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getActiveMerchantDocuments(RoutingContext ctx) {
        docQueryClient.findAllActive(buildFindAllDocReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getTrashedMerchantDocuments(RoutingContext ctx) {
        docQueryClient.findAllTrashed(buildFindAllDocReq(ctx))
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getMerchantDocumentById(RoutingContext ctx) {
        var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                .setDocumentId(GrpcGatewayUtils.getSafePathInt(ctx, "documentId"))
                .build();
        docQueryClient.findById(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    // Null-Safe Commands for Documents
    public void createMerchantDocument(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = MerchantDocumentCommand.CreateMerchantDocumentRequest.newBuilder()
                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
                .setDocumentType(GrpcGatewayUtils.getJsonString(body, "document_type", ""))
                .setDocumentUrl(GrpcGatewayUtils.getJsonString(body, "document_path", ""))
                .build();
        docCommandClient.create(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 201))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void updateMerchantDocument(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = MerchantDocumentCommand.UpdateMerchantDocumentRequest.newBuilder()
                .setDocumentId(GrpcGatewayUtils.getSafePathInt(ctx, "documentId"))
                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
                .setDocumentType(GrpcGatewayUtils.getJsonString(body, "document_type", ""))
                .setDocumentUrl(GrpcGatewayUtils.getJsonString(body, "document_path", ""))
                .build();
        docCommandClient.update(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void updateMerchantDocumentStatus(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest.newBuilder()
                .setDocumentId(GrpcGatewayUtils.getSafePathInt(ctx, "documentId"))
                .setStatus(GrpcGatewayUtils.getJsonString(body, "status", ""))
                .setNote(GrpcGatewayUtils.getJsonString(body, "note", ""))
                .build();
        docCommandClient.updateStatus(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void trashMerchantDocument(RoutingContext ctx) {
        var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                .setDocumentId(GrpcGatewayUtils.getSafePathInt(ctx, "documentId"))
                .build();
        docCommandClient.trashed(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restoreMerchantDocument(RoutingContext ctx) {
        var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                .setDocumentId(GrpcGatewayUtils.getSafePathInt(ctx, "documentId"))
                .build();
        docCommandClient.restore(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deleteMerchantDocumentPermanently(RoutingContext ctx) {
        var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                .setDocumentId(GrpcGatewayUtils.getSafePathInt(ctx, "documentId"))
                .build();
        docCommandClient.deletePermanent(req)
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restoreAllMerchantDocuments(RoutingContext ctx) {
        docCommandClient.restoreAll(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deleteAllPermanentMerchantDocuments(RoutingContext ctx) {
        docCommandClient.deleteAllPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(r -> GrpcGatewayUtils.sendResponse(ctx, r, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }
}