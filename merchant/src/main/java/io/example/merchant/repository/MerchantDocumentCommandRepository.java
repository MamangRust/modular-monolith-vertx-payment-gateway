package io.example.merchant.repository;

import io.example.merchant.model.MerchantDocument;
import io.vertx.core.Future;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;

public interface MerchantDocumentCommandRepository {
  Future<MerchantDocument> createMerchantDocument(CreateMerchantDocumentRequest request);

  Future<MerchantDocument> updateMerchantDocument(UpdateMerchantDocumentRequest request);

  Future<MerchantDocument> updateMerchantDocumentStatus(UpdateMerchantDocumentStatusRequest request);

  Future<MerchantDocument> trashedMerchantDocument(Integer merchantDocumentId);

  Future<MerchantDocument> restoreMerchantDocument(Integer merchantDocumentId);

  Future<Boolean> deleteMerchantDocumentPermanent(Integer merchantDocumentId);

  Future<Integer> restoreAllMerchantDocuments();

  Future<Integer> deleteAllMerchantDocumentsPermanent();
}
