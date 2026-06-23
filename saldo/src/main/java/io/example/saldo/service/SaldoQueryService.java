package io.example.saldo.service;

import io.example.common.domain.PagedResult;
import io.example.saldo.model.SaldoResponse;
import io.example.saldo.model.SaldoResponseDeleteAt;
import io.vertx.core.Future;
import pb.saldo.Saldo.FindAllSaldoRequest;

public interface SaldoQueryService {
  Future<PagedResult<SaldoResponse>> getAllSaldos(FindAllSaldoRequest req);

  Future<PagedResult<SaldoResponseDeleteAt>> getActiveSaldos(FindAllSaldoRequest req);

  Future<PagedResult<SaldoResponseDeleteAt>> getTrashedSaldos(FindAllSaldoRequest req);

  Future<SaldoResponse> getSaldoByCardNumber(String cardNumber);

  Future<SaldoResponse> getSaldoById(Integer saldoId);

}
