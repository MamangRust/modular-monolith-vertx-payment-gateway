package io.example.saldo.service;

import io.example.saldo.model.SaldoResponse;
import io.example.saldo.model.SaldoResponseDeleteAt;
import io.vertx.core.Future;
import io.example.saldo.domain.requests.CreateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoDeltaRequest;
import io.example.saldo.domain.requests.UpdateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoBalanceRequest;
import io.example.saldo.domain.requests.UpdateSaldoWithdrawRequest;

public interface SaldoCommandService {
  Future<SaldoResponse> createSaldo(CreateSaldoRequest req);

  Future<SaldoResponse> updateSaldo(UpdateSaldoRequest req);

  Future<SaldoResponseDeleteAt> trashSaldo(Integer saldoId);

  Future<SaldoResponse> updateSaldoBalance(UpdateSaldoBalanceRequest req);

  /**
   * Applies an atomic delta (positive = credit, negative = guarded debit).
   * Returns a business error when the saldo is missing or the debit would
   * exceed the current balance.
   */
  Future<SaldoResponse> updateSaldoDelta(UpdateSaldoDeltaRequest req);

  Future<SaldoResponse> updateSaldoWithdraw(UpdateSaldoWithdrawRequest req);

  Future<SaldoResponseDeleteAt> restoreSaldo(Integer saldoId);

  Future<Void> deleteSaldoPermanently(Integer saldoId);

  Future<Void> restoreAllSaldos();

  Future<Void> deleteAllPermanentSaldos();
}