package io.example.withdraw.service;

import io.example.withdraw.domain.requests.CreateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateWithdrawRequest;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.vertx.core.Future;

public interface WithdrawCommandService {
  Future<WithdrawResponse> createWithdraw(CreateWithdrawRequest req);

  Future<WithdrawResponse> updateWithdraw(UpdateWithdrawRequest req);

  Future<WithdrawResponseDeleteAt> trashWithdraw(Integer withdrawId);

  Future<WithdrawResponseDeleteAt> restoreWithdraw(Integer withdrawId);

  Future<Void> deleteWithdrawPermanently(Integer withdrawId);

  Future<Void> restoreAllWithdraws();

  Future<Void> deleteAllPermanentWithdraws();
}
