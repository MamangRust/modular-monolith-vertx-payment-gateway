package io.example.saldo.repository.impl;

import io.example.saldo.domain.requests.CreateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoBalanceRequest;
import io.example.saldo.domain.requests.UpdateSaldoWithdrawRequest;
import io.example.saldo.model.Saldo;
import io.example.saldo.repository.SaldoCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class SaldoCommandRepositoryImpl implements SaldoCommandRepository {
  private final Pool client;

  public SaldoCommandRepositoryImpl(Pool client) {
    this.client = client;
  }

  @Override
  public Future<Boolean> checkCardExists(String cardNumber) {
    return client.preparedQuery("SELECT COUNT(*) FROM cards WHERE card_number = $1 AND deleted_at IS NULL")
        .execute(Tuple.of(cardNumber))
        .map(rows -> rows.iterator().next().getLong(0) > 0);
  }

  @Override
  public Future<Saldo> createSaldo(CreateSaldoRequest req) {
    return client.preparedQuery("""
            INSERT INTO saldos (card_number, total_balance, created_at, updated_at)
            VALUES ($1, $2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING *
            """)
        .execute(Tuple.of(req.getCardNumber(), req.getTotalBalance()))
        .map(this::mapSingle);
  }

  @Override
  public Future<Saldo> updateSaldo(UpdateSaldoRequest req) {
    return client.preparedQuery("""
            UPDATE saldos SET card_number = $2, total_balance = $3, updated_at = CURRENT_TIMESTAMP
            WHERE saldo_id = $1 AND deleted_at IS NULL RETURNING *
            """)
        .execute(Tuple.of(req.getSaldoId(), req.getCardNumber(), req.getTotalBalance()))
        .map(this::mapSingle);
  }

  @Override
  public Future<Saldo> updateSaldoBalance(UpdateSaldoBalanceRequest req) {
    return client.preparedQuery("""
            UPDATE saldos SET total_balance = $2, updated_at = CURRENT_TIMESTAMP
            WHERE card_number = $1 AND deleted_at IS NULL RETURNING *
            """)
        .execute(Tuple.of(req.getCardNumber(), req.getTotalBalance()))
        .map(this::mapSingle);
  }

  @Override
  public Future<Saldo> updateSaldoWithdraw(UpdateSaldoWithdrawRequest req) {
    return client.preparedQuery("""
            UPDATE saldos
            SET withdraw_amount = $2, total_balance = total_balance - $2, withdraw_time = $3, updated_at = CURRENT_TIMESTAMP
            WHERE card_number = $1 AND deleted_at IS NULL AND total_balance >= $2 RETURNING *
            """)
        .execute(Tuple.of(req.getCardNumber(), req.getWithdrawAmount(), req.getWithdrawTime()))
        .map(this::mapSingle);
  }

  @Override
  public Future<Saldo> trash(Integer id) {
    return client.preparedQuery("UPDATE saldos SET deleted_at = CURRENT_TIMESTAMP WHERE saldo_id = $1 AND deleted_at IS NULL RETURNING *")
        .execute(Tuple.of(id))
        .map(this::mapSingle);
  }

  @Override
  public Future<Saldo> restore(Integer id) {
    return client.preparedQuery("UPDATE saldos SET deleted_at = NULL WHERE saldo_id = $1 AND deleted_at IS NOT NULL RETURNING *")
        .execute(Tuple.of(id))
        .map(this::mapSingle);
  }

  @Override
  public Future<Void> deletePermanent(Integer id) {
    return client.preparedQuery("DELETE FROM saldos WHERE saldo_id = $1 AND deleted_at IS NOT NULL")
        .execute(Tuple.of(id))
        .mapEmpty();
  }

  @Override
  public Future<Void> restoreAll() {
    return client.query("UPDATE saldos SET deleted_at = NULL WHERE deleted_at IS NOT NULL").execute().mapEmpty();
  }

  @Override
  public Future<Void> deleteAllPermanent() {
    return client.query("DELETE FROM saldos WHERE deleted_at IS NOT NULL").execute().mapEmpty();
  }

  private Saldo mapSingle(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Saldo.fromRow(rows.iterator().next()) : null;
  }
}
