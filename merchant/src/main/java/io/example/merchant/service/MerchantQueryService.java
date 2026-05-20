package io.example.merchant.service;

import java.util.List;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.merchant.model.MerchantResponse;
import io.example.merchant.model.MerchantResponseDeleteAt;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindAllMerchantRequest;

public interface MerchantQueryService {
  Future<ApiResponsePagination<List<MerchantResponse>>> findAll(FindAllMerchantRequest req);

  Future<ApiResponse<MerchantResponse>> findById(int merchantId);

  Future<ApiResponsePagination<List<MerchantResponseDeleteAt>>> findByActive(FindAllMerchantRequest req);

  Future<ApiResponsePagination<List<MerchantResponseDeleteAt>>> findByTrashed(FindAllMerchantRequest req);

  Future<ApiResponse<MerchantResponse>> findByApiKey(String apiKey);

  Future<ApiResponse<List<MerchantResponse>>> findByMerchantUserId(int userId);
}
