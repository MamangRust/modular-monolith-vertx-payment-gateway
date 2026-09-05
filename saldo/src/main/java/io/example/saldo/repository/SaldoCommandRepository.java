package io.example.saldo.repository;

import io.example.saldo.domain.requests.CreateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoDeltaRequest;
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

  /**
   * Atomically applies a delta to the balance of the given card. A negative
   * delta (debit) only succeeds when the resulting balance stays &gt;= 0;
   * otherwise the update is rejected and {@code null} is returned. This is the
   * race-free replacement for read-modify-write absolute updates.
   */
  Future<Saldo> updateSaldoDelta(UpdateSaldoDeltaRequest req);

  Future<Saldo> updateSaldoWithdraw(UpdateSaldoWithdrawRequest req);

  Future<Saldo> trash(Integer id);

  Future<Saldo> restore(Integer id);

  Future<Boolean> deletePermanent(Integer id);

  Future<Integer> restoreAll();

  Future<Integer> deleteAllPermanent();
}
