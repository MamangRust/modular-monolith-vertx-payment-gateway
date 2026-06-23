package io.example.merchant.service;

import io.example.common.domain.PagedResult;
import io.example.merchant.model.MerchantDocumentResponse;
import io.example.merchant.model.MerchantDocumentResponseDeleteAt;
import io.vertx.core.Future;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;

public interface MerchantDocumentQueryService {
  Future<PagedResult<MerchantDocumentResponse>> findAll(FindAllMerchantDocumentsRequest req);

  Future<PagedResult<MerchantDocumentResponseDeleteAt>> findByActive(FindAllMerchantDocumentsRequest req);

  Future<PagedResult<MerchantDocumentResponseDeleteAt>> findByTrashed(FindAllMerchantDocumentsRequest req);

  Future<MerchantDocumentResponse> findById(Integer documentId);
}