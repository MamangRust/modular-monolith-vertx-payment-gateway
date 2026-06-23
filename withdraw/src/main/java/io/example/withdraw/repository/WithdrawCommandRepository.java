package io.example.withdraw.repository;

import io.example.withdraw.domain.requests.CreateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateWithdrawStatus;
import io.example.withdraw.model.Withdraw;
import io.vertx.core.Future;

public interface WithdrawCommandRepository {
  Future<Withdraw> createWithdraw(CreateWithdrawRequest req);

  Future<Withdraw> updateWithdraw(UpdateWithdrawRequest req);

  Future<Withdraw> updateWithdrawStatus(UpdateWithdrawStatus req);

  Future<Withdraw> trashWithdraw(Integer id);

  Future<Withdraw> restoreWithdraw(Integer id);

  Future<Boolean> deleteWithdrawPermanently(Integer id);

  Future<Integer> restoreAllWithdraws();

  Future<Integer> deleteAllPermanentWithdraws();
}
