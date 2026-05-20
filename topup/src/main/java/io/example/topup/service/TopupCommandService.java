package io.example.topup.service;

import io.example.common.model.ApiResponse;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.vertx.core.Future;

public interface TopupCommandService {
  Future<ApiResponse<TopupResponse>> createTopup(pb.topup.TopupCommand.CreateTopupRequest req);
  Future<ApiResponse<TopupResponse>> updateTopup(pb.topup.TopupCommand.UpdateTopupRequest req);
  Future<ApiResponse<TopupResponseDeleteAt>> trashTopup(Integer topupId);
  Future<ApiResponse<TopupResponseDeleteAt>> restoreTopup(Integer topupId);
  Future<ApiResponse<Void>> deleteTopupPermanently(Integer topupId);
  Future<ApiResponse<Void>> restoreAllTopups();
  Future<ApiResponse<Void>> deleteAllPermanentTopups();
}
