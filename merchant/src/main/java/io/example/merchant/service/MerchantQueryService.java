package io.example.merchant.service;

import java.util.List;
import io.example.common.domain.PagedResult;
import io.example.merchant.model.MerchantResponse;
import io.example.merchant.model.MerchantResponseDeleteAt;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindAllMerchantRequest;

public interface MerchantQueryService {
  Future<PagedResult<MerchantResponse>> findAll(FindAllMerchantRequest req);

  Future<MerchantResponse> findById(int merchantId);

  Future<PagedResult<MerchantResponseDeleteAt>> findByActive(FindAllMerchantRequest req);

  Future<PagedResult<MerchantResponseDeleteAt>> findByTrashed(FindAllMerchantRequest req);

  Future<MerchantResponse> findByApiKey(String apiKey);

  Future<List<MerchantResponse>> findByMerchantUserId(Integer userId);
}