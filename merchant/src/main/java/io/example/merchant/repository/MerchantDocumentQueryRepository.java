package io.example.merchant.repository;

import io.example.common.domain.PagedResult;
import io.example.merchant.model.MerchantDocument;
import io.vertx.core.Future;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;

public interface MerchantDocumentQueryRepository {
  Future<PagedResult<MerchantDocument>> findAllDocuments(FindAllMerchantDocumentsRequest request);

  Future<MerchantDocument> findByIdDocument(int id);

  Future<PagedResult<MerchantDocument>> findByActiveDocuments(FindAllMerchantDocumentsRequest request);

  Future<PagedResult<MerchantDocument>> findByTrashedDocuments(FindAllMerchantDocumentsRequest request);
}
