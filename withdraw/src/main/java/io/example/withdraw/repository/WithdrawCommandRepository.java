package io.example.withdraw.repository;

import io.example.withdraw.model.Withdraw;
import io.vertx.core.Future;

public interface WithdrawCommandRepository {
  Future<Withdraw> createWithdraw(String card, long amount);

  Future<Withdraw> updateWithdraw(int id, String card, long amount);

  Future<Withdraw> updateWithdrawStatus(int id, String status);

  Future<Withdraw> trashWithdraw(int id);

  Future<Withdraw> restoreWithdraw(int id);

  Future<Void> deleteWithdrawPermanently(int id);

  Future<Void> restoreAllWithdraws();

  Future<Void> deleteAllPermanentWithdraws();
}
