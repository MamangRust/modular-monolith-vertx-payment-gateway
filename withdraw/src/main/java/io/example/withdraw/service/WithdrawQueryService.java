package io.example.withdraw.service;

import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.withdraw.domain.requests.FindAllWithdraws;
import io.example.withdraw.domain.requests.FindAllWithdrawCardNumber;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.vertx.core.Future;

public interface WithdrawQueryService {
  Future<PagedResult<WithdrawResponse>> getWithdraws(FindAllWithdraws req);

  Future<PagedResult<WithdrawResponse>> getWithdrawsByCardNumber(FindAllWithdrawCardNumber req);

  Future<PagedResult<WithdrawResponseDeleteAt>> getActiveWithdraws(FindAllWithdraws req);

  Future<PagedResult<WithdrawResponseDeleteAt>> getTrashedWithdraws(FindAllWithdraws req);

  Future<WithdrawResponse> getWithdrawById(Integer withdrawId);

  Future<List<WithdrawResponse>> getWithdrawsByCardNumberPrimitive(String cardNumber);
}