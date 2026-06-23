package io.example.saldo.repository;

import io.example.common.domain.PagedResult;
import io.example.saldo.domain.requests.FindAllSaldos;
import io.example.saldo.model.Saldo;
import io.vertx.core.Future;

public interface SaldoQueryRepository {
  Future<PagedResult<Saldo>> getSaldos(FindAllSaldos req);

  Future<PagedResult<Saldo>> getActiveSaldos(FindAllSaldos req);

  Future<PagedResult<Saldo>> getTrashedSaldos(FindAllSaldos req);

  Future<Saldo> getSaldoById(Integer id);

  Future<Saldo> findByTrashedId(Integer id);

  Future<Saldo> getSaldoByCardNumber(String cardNumber);
}
