package io.example.merchant.service;

import java.util.List;

import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.merchant.model.MerchantDocumentResponse;
import io.example.merchant.model.MerchantDocumentResponseDeleteAt;
import io.vertx.core.Future;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;

public interface MerchantDocumentQueryService {
  Future<ApiResponsePagination<List<MerchantDocumentResponse>>> findAll(FindAllMerchantDocumentsRequest req);

  Future<ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>>> findByActive(
      FindAllMerchantDocumentsRequest req);

  Future<ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>>> findByTrashed(
      FindAllMerchantDocumentsRequest req);

  Future<ApiResponse<MerchantDocumentResponse>> findById(int documentId);
}
