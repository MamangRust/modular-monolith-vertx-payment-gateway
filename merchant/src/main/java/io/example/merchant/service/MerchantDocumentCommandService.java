package io.example.merchant.service;

import io.example.merchant.model.MerchantDocumentResponse;
import io.example.merchant.model.MerchantDocumentResponseDeleteAt;
import io.vertx.core.Future;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;

public interface MerchantDocumentCommandService {
  Future<MerchantDocumentResponse> createMerchantDocument(CreateMerchantDocumentRequest request);

  Future<MerchantDocumentResponse> updateMerchantDocument(UpdateMerchantDocumentRequest request);

  Future<MerchantDocumentResponse> updateMerchantDocumentStatus(UpdateMerchantDocumentStatusRequest request);

  Future<MerchantDocumentResponseDeleteAt> trashedMerchantDocument(Integer documentId);

  Future<MerchantDocumentResponseDeleteAt> restoreMerchantDocument(Integer documentId);

  Future<Void> deleteMerchantDocumentPermanent(Integer documentId);

  Future<Void> restoreAllMerchantDocument();

  Future<Void> deleteAllMerchantDocumentPermanent();
}