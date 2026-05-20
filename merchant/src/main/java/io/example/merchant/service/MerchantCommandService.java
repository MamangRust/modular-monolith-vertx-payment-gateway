package io.example.merchant.service;

import io.example.common.model.ApiResponse;
import io.example.merchant.model.MerchantResponse;
import io.example.merchant.model.MerchantResponseDeleteAt;
import io.vertx.core.Future;
import pb.merchant.MerchantCommand.CreateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantStatusRequest;

public interface MerchantCommandService {
  Future<ApiResponse<MerchantResponse>> createMerchant(CreateMerchantRequest request);
  Future<ApiResponse<MerchantResponse>> updateMerchant(UpdateMerchantRequest request);
  Future<ApiResponse<MerchantResponse>> updateMerchantStatus(UpdateMerchantStatusRequest request);
  
  Future<ApiResponse<MerchantResponseDeleteAt>> trashedMerchant(int merchantId);
  Future<ApiResponse<MerchantResponseDeleteAt>> restoreMerchant(int merchantId);
  Future<ApiResponse<Boolean>> deleteMerchantPermanent(int merchantId);
  
  Future<ApiResponse<Boolean>> restoreAllMerchant();
  Future<ApiResponse<Boolean>> deleteAllMerchantPermanent();
}
