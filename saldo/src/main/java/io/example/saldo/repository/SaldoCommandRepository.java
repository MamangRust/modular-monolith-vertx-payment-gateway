package io.example.saldo.repository;

import io.example.saldo.domain.requests.CreateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoBalanceRequest;
import io.example.saldo.domain.requests.UpdateSaldoWithdrawRequest;
import io.example.saldo.model.Saldo;
import io.vertx.core.Future;

public interface SaldoCommandRepository {
  Future<Boolean> checkCardExists(String cardNumber);

  Future<Saldo> createSaldo(CreateSaldoRequest req);

  Future<Saldo> updateSaldo(UpdateSaldoRequest req);

  Future<Saldo> updateSaldoBalance(UpdateSaldoBalanceRequest req);

  Future<Saldo> updateSaldoWithdraw(UpdateSaldoWithdrawRequest req);

  Future<Saldo> trash(Integer id);

  Future<Saldo> restore(Integer id);

  Future<Boolean> deletePermanent(Integer id);

  Future<Integer> restoreAll();

  Future<Integer> deleteAllPermanent();
}
