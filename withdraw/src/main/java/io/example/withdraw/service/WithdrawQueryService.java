package io.example.withdraw.service;

import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.vertx.core.Future;
import pb.withdraw.Withdraw.FindAllWithdrawRequest;
import pb.withdraw.Withdraw.FindAllWithdrawByCardNumberRequest;

import java.util.List;

public interface WithdrawQueryService {
  Future<ApiResponsePagination<List<WithdrawResponse>>> getWithdraws(FindAllWithdrawRequest req);

  Future<ApiResponsePagination<List<WithdrawResponse>>> getWithdrawsByCardNumber(FindAllWithdrawByCardNumberRequest req);

  Future<ApiResponsePagination<List<WithdrawResponseDeleteAt>>> getActiveWithdraws(FindAllWithdrawRequest req);

  Future<ApiResponsePagination<List<WithdrawResponseDeleteAt>>> getTrashedWithdraws(FindAllWithdrawRequest req);

  Future<ApiResponse<WithdrawResponse>> getWithdrawById(Integer withdrawId);

  Future<ApiResponse<List<WithdrawResponse>>> getWithdrawsByCardNumberPrimitive(String cardNumber);
}
