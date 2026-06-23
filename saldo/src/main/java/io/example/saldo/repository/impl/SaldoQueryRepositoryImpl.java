package io.example.saldo.repository.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.saldo.domain.requests.FindAllSaldos;
import io.example.saldo.model.Saldo;
import io.example.saldo.repository.SaldoQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SaldoQueryRepositoryImpl implements SaldoQueryRepository {
  private final Pool client;

  @Override
  public Future<PagedResult<Saldo>> getSaldos(FindAllSaldos req) {
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    int offset = Math.max(0, req.getPage() - 1) * pageSize;
    return client.preparedQuery("""
        SELECT *, COUNT(*) OVER() AS total_count
        FROM saldos
        WHERE deleted_at IS NULL AND ($1::TEXT IS NULL OR card_number ILIKE '%' || $1 || '%')
        ORDER BY saldo_id LIMIT $2 OFFSET $3
        """)
        .execute(Tuple.of(normalize(req.getSearch()), pageSize, offset))
        .map(this::mapPagedSaldos);
  }

  @Override
  public Future<PagedResult<Saldo>> getActiveSaldos(FindAllSaldos req) {
    return getSaldos(req);
  }

  @Override
  public Future<PagedResult<Saldo>> getTrashedSaldos(FindAllSaldos req) {
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    int offset = Math.max(0, req.getPage() - 1) * pageSize;
    return client.preparedQuery("""
        SELECT *, COUNT(*) OVER() AS total_count
        FROM saldos
        WHERE deleted_at IS NOT NULL AND ($1::TEXT IS NULL OR card_number ILIKE '%' || $1 || '%')
        ORDER BY saldo_id LIMIT $2 OFFSET $3
        """)
        .execute(Tuple.of(normalize(req.getSearch()), pageSize, offset))
        .map(this::mapPagedSaldos);
  }

  @Override
  public Future<Saldo> getSaldoById(Integer id) {
    return client.preparedQuery("SELECT * FROM saldos WHERE saldo_id = $1 AND deleted_at IS NULL")
        .execute(Tuple.of(id))
        .map(this::mapSingle);
  }

  @Override
  public Future<Saldo> findByTrashedId(Integer id) {
    return client.preparedQuery("SELECT * FROM saldos WHERE saldo_id = $1 AND deleted_at IS NOT NULL")
        .execute(Tuple.of(id))
        .map(this::mapSingle);
  }

  @Override
  public Future<Saldo> getSaldoByCardNumber(String cardNumber) {
    return client.preparedQuery("SELECT * FROM saldos WHERE card_number = $1 AND deleted_at IS NULL")
        .execute(Tuple.of(cardNumber))
        .map(this::mapSingle);
  }

  private String normalize(String input) {
    return (input == null || input.isBlank()) ? null : input.trim();
  }

  private Saldo mapSingle(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Saldo.fromRow(rows.iterator().next()) : null;
  }

  private PagedResult<Saldo> mapPagedSaldos(RowSet<Row> rows) {
    List<Saldo> list = new ArrayList<>();
    int totalCount = 0;
    for (Row row : rows) {
      list.add(Saldo.fromRow(row));
      if (totalCount == 0) {
        Integer c = row.getInteger("total_count");
        totalCount = (c != null) ? c : 0;
      }
    }
    return new PagedResult<>(list, totalCount);
  }
}
