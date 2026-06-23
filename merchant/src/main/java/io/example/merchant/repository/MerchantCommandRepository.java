package io.example.merchant.repository;

import io.example.merchant.model.Merchant;
import io.vertx.core.Future;
import pb.merchant.MerchantCommand.CreateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantStatusRequest;

public interface MerchantCommandRepository {
  Future<Merchant> createMerchant(CreateMerchantRequest request);

  Future<Merchant> updateMerchant(UpdateMerchantRequest request);

  Future<Merchant> updateMerchantStatus(UpdateMerchantStatusRequest request);

  Future<Merchant> trashedMerchant(Integer merchantId);

  Future<Merchant> restoreMerchant(Integer merchantId);

  Future<Boolean> deleteMerchantPermanent(Integer merchantId);

  Future<Integer> restoreAllMerchants();

  Future<Integer> deleteAllMerchantsPermanent();
}
