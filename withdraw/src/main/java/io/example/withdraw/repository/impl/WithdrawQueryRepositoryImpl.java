package io.example.withdraw.repository.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.withdraw.domain.requests.FindAllWithdraws;
import io.example.withdraw.model.Withdraw;
import io.example.withdraw.repository.WithdrawQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WithdrawQueryRepositoryImpl implements WithdrawQueryRepository {
  private final Pool pool;

  private String normalizeSearch(String search) {
    return (search == null || search.isBlank()) ? null : search;
  }

  @Override
  public Future<PagedResult<Withdraw>> getWithdraws(FindAllWithdraws req) {
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    int offset = (page - 1) * pageSize;

    String sql = """
        SELECT *, COUNT(*) OVER() AS total_count FROM withdraws
        WHERE deleted_at IS NULL
          AND ($1::TEXT IS NULL
            OR card_number ILIKE '%' || $1 || '%'
            OR status ILIKE '%' || $1 || '%'
          )
        ORDER BY withdraw_time DESC LIMIT $2 OFFSET $3
        """;

    return pool.preparedQuery(sql)
        .execute(Tuple.of(normalizeSearch(req.getSearch()), pageSize, offset))
        .map(this::mapPagedWithdraws);
  }

  @Override
  public Future<PagedResult<Withdraw>> getActiveWithdraws(FindAllWithdraws req) {
    return getWithdraws(req);
  }

  @Override
  public Future<PagedResult<Withdraw>> getTrashedWithdraws(FindAllWithdraws req) {
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    int offset = (page - 1) * pageSize;

    String sql = """
        SELECT *, COUNT(*) OVER() AS total_count FROM withdraws
        WHERE deleted_at IS NOT NULL
          AND ($1::TEXT IS NULL OR card_number ILIKE '%' || $1 || '%')
        ORDER BY withdraw_time DESC LIMIT $2 OFFSET $3
        """;

    return pool.preparedQuery(sql)
        .execute(Tuple.of(normalizeSearch(req.getSearch()), pageSize, offset))
        .map(this::mapPagedWithdraws);
  }

  @Override
  public Future<Withdraw> getWithdrawById(int id) {
    String sql = "SELECT * FROM withdraws WHERE withdraw_id = $1 AND deleted_at IS NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(id)).map(this::mapSingleOrNull);
  }

  @Override
  public Future<Withdraw> findByTrashed(Integer withdrawId) {
    return pool.preparedQuery(
        "SELECT * FROM withdraws WHERE withdraw_id = $1 AND deleted_at IS NOT NULL")
        .execute(Tuple.of(withdrawId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<PagedResult<Withdraw>> getWithdrawsByCardNumber(String card, String search, int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    String sql = """
        SELECT *, COUNT(*) OVER() AS total_count FROM withdraws
        WHERE deleted_at IS NULL
          AND card_number = $1
          AND ($2::TEXT IS NULL OR status ILIKE '%' || $2 || '%')
        ORDER BY withdraw_time DESC LIMIT $3 OFFSET $4
        """;
    return pool.preparedQuery(sql)
        .execute(Tuple.of(card, normalizeSearch(search), pageSize, offset))
        .map(this::mapPagedWithdraws);
  }

  @Override
  public Future<Long> getTodaySuccessfulAmount(String card) {
    String sql = """
        SELECT COALESCE(SUM(withdraw_amount), 0) AS total_amount
        FROM withdraws
        WHERE card_number = $1 AND status = 'success' AND deleted_at IS NULL
          AND withdraw_time >= CURRENT_DATE
          AND withdraw_time < CURRENT_DATE + INTERVAL '1 day'
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(card))
        .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getLong("total_amount") : 0L);
  }

  @Override
  public Future<List<Withdraw>> getWithdrawsByCardNumberPrimitive(String card) {
    String sql = "SELECT * FROM withdraws WHERE deleted_at IS NULL AND card_number = $1 ORDER BY withdraw_time DESC";
    return pool.preparedQuery(sql).execute(Tuple.of(card)).map(this::mapList);
  }

  private Withdraw mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Withdraw.fromRow(rows.iterator().next()) : null;
  }

  private List<Withdraw> mapList(RowSet<Row> rows) {
    List<Withdraw> list = new ArrayList<>();
    for (Row r : rows) {
      list.add(Withdraw.fromRow(r));
    }
    return list;
  }

  private PagedResult<Withdraw> mapPagedWithdraws(RowSet<Row> rows) {
    List<Withdraw> list = new ArrayList<>();
    int total = 0;
    for (Row row : rows) {
      list.add(Withdraw.fromRow(row));
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
