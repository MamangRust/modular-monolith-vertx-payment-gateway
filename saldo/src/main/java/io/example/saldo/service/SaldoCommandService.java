package io.example.saldo.service;

import io.example.common.model.ApiResponse;
import io.example.saldo.model.SaldoResponse;
import io.example.saldo.model.SaldoResponseDeleteAt;
import io.vertx.core.Future;
import io.example.saldo.domain.requests.CreateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoBalanceRequest;
import io.example.saldo.domain.requests.UpdateSaldoWithdrawRequest;

public interface SaldoCommandService {
  Future<ApiResponse<SaldoResponse>> createSaldo(CreateSaldoRequest req);
  Future<ApiResponse<SaldoResponse>> updateSaldo(UpdateSaldoRequest req);
  Future<ApiResponse<SaldoResponseDeleteAt>> trashSaldo(Integer saldoId);
  Future<ApiResponse<SaldoResponse>> updateSaldoBalance(UpdateSaldoBalanceRequest req);
  Future<ApiResponse<SaldoResponse>> updateSaldoWithdraw(UpdateSaldoWithdrawRequest req);
  Future<ApiResponse<SaldoResponseDeleteAt>> restoreSaldo(Integer saldoId);
  Future<ApiResponse<Void>> deleteSaldoPermanently(Integer saldoId);
  Future<ApiResponse<Void>> restoreAllSaldos();
  Future<ApiResponse<Void>> deleteAllPermanentSaldos();
}
