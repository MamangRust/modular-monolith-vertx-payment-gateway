package io.example.transfer.repository.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.common.model.PagedResult;
import io.example.transfer.domain.requests.FindAllTransfers;
import io.example.transfer.model.Transfer;
import io.example.transfer.repository.TransferQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class TransferQueryRepositoryImpl implements TransferQueryRepository {
  private final Pool pool;

  public TransferQueryRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  private String normalizeSearch(String search) {
    return (search == null || search.isBlank()) ? null : search;
  }

  @Override
  public Future<PagedResult<Transfer>> getTransfers(FindAllTransfers req) {
    int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
    int offset = page * req.getPageSize();
    String sql = """
        SELECT *, COUNT(*) OVER() AS total_count FROM transfers
        WHERE deleted_at IS NULL
          AND ($1::TEXT IS NULL OR transfer_from ILIKE '%' || $1 || '%' OR transfer_to ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%')
        ORDER BY transfer_time DESC LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
        .map(this::mapPagedTransfers);
  }

  @Override
  public Future<PagedResult<Transfer>> getActiveTransfers(FindAllTransfers req) {
    return getTransfers(req);
  }

  @Override
  public Future<PagedResult<Transfer>> getTrashedTransfers(FindAllTransfers req) {
    int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
    int offset = page * req.getPageSize();
    String sql = """
        SELECT *, COUNT(*) OVER() AS total_count FROM transfers
        WHERE deleted_at IS NOT NULL
          AND ($1::TEXT IS NULL OR transfer_from ILIKE '%' || $1 || '%' OR transfer_to ILIKE '%' || $1 || '%')
        ORDER BY transfer_time DESC LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
        .map(this::mapPagedTransfers);
  }

  @Override
  public Future<Transfer> getTransferById(int id) {
    String sql = "SELECT * FROM transfers WHERE transfer_id = $1 AND deleted_at IS NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(id)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<List<Transfer>> getTransfersByCardNumber(String card) {
    String sql = "SELECT * FROM transfers WHERE deleted_at IS NULL AND (transfer_from = $1 OR transfer_to = $1) ORDER BY transfer_time DESC";
    return pool.preparedQuery(sql).execute(Tuple.of(card)).map(this::mapList);
  }

  @Override
  public Future<List<Transfer>> getTransfersBySender(String card) {
    String sql = "SELECT * FROM transfers WHERE deleted_at IS NULL AND transfer_from = $1 ORDER BY transfer_time DESC";
    return pool.preparedQuery(sql).execute(Tuple.of(card)).map(this::mapList);
  }

  @Override
  public Future<List<Transfer>> getTransfersByReceiver(String card) {
    String sql = "SELECT * FROM transfers WHERE deleted_at IS NULL AND transfer_to = $1 ORDER BY transfer_time DESC";
    return pool.preparedQuery(sql).execute(Tuple.of(card)).map(this::mapList);
  }

  private Transfer mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Transfer.fromRow(rows.iterator().next()) : null;
  }

  private List<Transfer> mapList(RowSet<Row> rows) {
    List<Transfer> list = new ArrayList<>();
    for (Row r : rows)
      list.add(Transfer.fromRow(r));
    return list;
  }

  private PagedResult<Transfer> mapPagedTransfers(RowSet<Row> rows) {
    List<Transfer> list = new ArrayList<>();
    int total = 0;
    for (Row row : rows) {
      list.add(Transfer.fromRow(row));
      if (total == 0) {
        Object tcVal = row.getValue("total_count");
        if (tcVal instanceof Number num)
          total = num.intValue();
      }
    }
    return new PagedResult<>(list, total);
  }
}
