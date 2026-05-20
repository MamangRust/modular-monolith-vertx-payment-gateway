package io.example.withdraw.service;

import io.example.common.model.ApiResponse;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.vertx.core.Future;
import pb.withdraw.WithdrawCommand.CreateWithdrawRequest;
import pb.withdraw.WithdrawCommand.UpdateWithdrawRequest;

public interface WithdrawCommandService {
  Future<ApiResponse<WithdrawResponse>> createWithdraw(CreateWithdrawRequest req);

  Future<ApiResponse<WithdrawResponse>> updateWithdraw(UpdateWithdrawRequest req);

  Future<ApiResponse<WithdrawResponseDeleteAt>> trashWithdraw(Integer withdrawId);

  Future<ApiResponse<WithdrawResponseDeleteAt>> restoreWithdraw(Integer withdrawId);

  Future<ApiResponse<Void>> deleteWithdrawPermanently(Integer withdrawId);

  Future<ApiResponse<Void>> restoreAllWithdraws();

  Future<ApiResponse<Void>> deleteAllPermanentWithdraws();
}
