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
  
  Future<Merchant> trashedMerchant(int merchantId);
  Future<Merchant> restoreMerchant(int merchantId);
  Future<Boolean> deleteMerchantPermanent(int merchantId);
  
  Future<Boolean> restoreAllMerchants();
  Future<Boolean> deleteAllMerchantsPermanent();
}
