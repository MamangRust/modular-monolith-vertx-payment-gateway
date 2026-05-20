package io.example.topup.repository.impl;

import io.example.common.domain.PagedResult;
import io.example.topup.model.Topup;
import io.example.topup.repository.TopupQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

import io.example.topup.domain.requests.topup.FindAllTopups;

public class TopupQueryRepositoryImpl implements TopupQueryRepository {
  private final Pool pool;

  public TopupQueryRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  private String normalizeSearch(String search) {
    return (search == null || search.isBlank()) ? null : search;
  }

  @Override
  public Future<PagedResult<Topup>> getTopups(FindAllTopups req) {
    int offset = Math.max(0, req.getPage() - 1) * req.getPageSize();
    String sql = """
        SELECT *, COUNT(*) OVER() AS total_count FROM topups
        WHERE deleted_at IS NULL
          AND ($1::TEXT IS NULL OR card_number ILIKE '%' || $1 || '%' OR topup_no ILIKE '%' || $1 || '%' OR topup_method ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%')
        ORDER BY topup_time DESC LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
        .map(this::mapPagedTopups);
  }

  @Override
  public Future<PagedResult<Topup>> getActiveTopups(FindAllTopups req) {
    return getTopups(req);
  }

  @Override
  public Future<PagedResult<Topup>> getTrashedTopups(FindAllTopups req) {
    int offset = Math.max(0, req.getPage() - 1) * req.getPageSize();
    String sql = """
        SELECT *, COUNT(*) OVER() AS total_count FROM topups
        WHERE deleted_at IS NOT NULL
          AND ($1::TEXT IS NULL OR card_number ILIKE '%' || $1 || '%' OR topup_no ILIKE '%' || $1 || '%' OR topup_method ILIKE '%' || $1 || '%')
        ORDER BY topup_time DESC LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
        .map(this::mapPagedTopups);
  }

  @Override
  public Future<Topup> getTopupById(int id) {
    String sql = "SELECT * FROM topups WHERE topup_id = $1 AND deleted_at IS NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(id)).map(this::mapSingleOrNull);
  }

  private Topup mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Topup.fromRow(rows.iterator().next()) : null;
  }

  private PagedResult<Topup> mapPagedTopups(RowSet<Row> rows) {
    List<Topup> list = new ArrayList<>();
    int total = 0;
    for (Row row : rows) {
      list.add(Topup.fromRow(row));
      if (total == 0) {
        Object tcVal = row.getValue("total_count");
        if (tcVal instanceof Number num) {
            total = num.intValue();
        }
      }
    }
    return new PagedResult<>(list, total);
  }
}
