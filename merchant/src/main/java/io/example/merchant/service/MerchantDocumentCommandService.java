package io.example.merchant.service;

import io.example.common.model.ApiResponse;
import io.example.merchant.model.MerchantDocumentResponse;
import io.example.merchant.model.MerchantDocumentResponseDeleteAt;
import io.vertx.core.Future;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;

public interface MerchantDocumentCommandService {
  Future<ApiResponse<MerchantDocumentResponse>> createMerchantDocument(CreateMerchantDocumentRequest request);

  Future<ApiResponse<MerchantDocumentResponse>> updateMerchantDocument(UpdateMerchantDocumentRequest request);

  Future<ApiResponse<MerchantDocumentResponse>> updateMerchantDocumentStatus(
      UpdateMerchantDocumentStatusRequest request);

  Future<ApiResponse<MerchantDocumentResponseDeleteAt>> trashedMerchantDocument(int documentId);

  Future<ApiResponse<MerchantDocumentResponseDeleteAt>> restoreMerchantDocument(int documentId);

  Future<ApiResponse<Boolean>> deleteMerchantDocumentPermanent(int documentId);

  Future<ApiResponse<Boolean>> restoreAllMerchantDocument();

  Future<ApiResponse<Boolean>> deleteAllMerchantDocumentPermanent();
}
