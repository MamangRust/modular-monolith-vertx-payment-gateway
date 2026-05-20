package io.example.withdraw.repository;

import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.withdraw.domain.requests.FindAllWithdraws;
import io.example.withdraw.model.Withdraw;
import io.vertx.core.Future;

public interface WithdrawQueryRepository {
  Future<PagedResult<Withdraw>> getWithdraws(FindAllWithdraws req);

  Future<PagedResult<Withdraw>> getActiveWithdraws(FindAllWithdraws req);

  Future<PagedResult<Withdraw>> getTrashedWithdraws(FindAllWithdraws req);

  Future<Withdraw> getWithdrawById(int id);

  Future<PagedResult<Withdraw>> getWithdrawsByCardNumber(String card, String search, int page, int pageSize);

  Future<List<Withdraw>> getWithdrawsByCardNumberPrimitive(String card);
}
