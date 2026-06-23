package io.example.merchant.service;

import io.example.merchant.model.MerchantResponse;
import io.example.merchant.model.MerchantResponseDeleteAt;
import io.vertx.core.Future;
import pb.merchant.MerchantCommand.CreateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantStatusRequest;

public interface MerchantCommandService {
  Future<MerchantResponse> createMerchant(CreateMerchantRequest request);

  Future<MerchantResponse> updateMerchant(UpdateMerchantRequest request);

  Future<MerchantResponse> updateMerchantStatus(UpdateMerchantStatusRequest request);

  Future<MerchantResponseDeleteAt> trashedMerchant(Integer merchantId);

  Future<MerchantResponseDeleteAt> restoreMerchant(Integer merchantId);

  Future<Void> deleteMerchantPermanent(Integer merchantId);

  Future<Void> restoreAllMerchant();

  Future<Void> deleteAllMerchantPermanent();
}