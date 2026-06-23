package io.example.merchant.repository;

import io.example.common.domain.PagedResult;
import io.example.merchant.model.Merchant;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindAllMerchantRequest;

public interface MerchantQueryRepository {
  Future<PagedResult<Merchant>> findAllMerchants(FindAllMerchantRequest request);

  Future<PagedResult<Merchant>> findByActive(FindAllMerchantRequest request);

  Future<PagedResult<Merchant>> findByTrashed(FindAllMerchantRequest request);

  Future<Merchant> findByApiKey(String apiKey);

  Future<Merchant> findByMerchantId(Integer merchantId);

  Future<Merchant> findByName(String name);

  Future<Merchant> findByTrashedById(Integer merchantId);

  Future<Merchant> findByRestoredById(Integer merchantId);

  Future<java.util.List<Merchant>> findByMerchantUserId(Integer userId);
}
