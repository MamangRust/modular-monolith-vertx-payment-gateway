package io.example.transfer.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import io.example.transfer.model.TransferStats;
import io.example.transfer.repository.TransferStatsAmountRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransferStatsAmountRepositoryImpl implements TransferStatsAmountRepository {
  private final Pool pool;

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  @Override
  public Future<List<TransferStats.MonthAmount>> getMonthlyTransferAmounts(int year) {
    String sql = """
        WITH months AS (
            SELECT generate_series(date_trunc('year', $1::timestamp), date_trunc('year', $1::timestamp) + interval '11 months', interval '1 month') AS month
        )
        SELECT TO_CHAR(m.month, 'Mon') AS month, COALESCE(SUM(t.transfer_amount), 0)::bigint AS total_transfer_amount
        FROM months m
        LEFT JOIN transfers t ON t.transfer_time >= m.month
            AND t.transfer_time < m.month + interval '1 month'
            AND t.deleted_at IS NULL
        GROUP BY m.month ORDER BY m.month
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(getYearStart(year)))
        .map(rows -> {
          List<TransferStats.MonthAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TransferStats.MonthAmount.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<TransferStats.YearAmount>> getYearlyTransferAmounts(int endYear) {
    String sql = """
        SELECT EXTRACT(YEAR FROM t.transfer_time)::integer AS year, COALESCE(SUM(t.transfer_amount), 0)::bigint AS total_transfer_amount
        FROM transfers t WHERE t.deleted_at IS NULL
          AND t.transfer_time >= (CAST($1 - 4 AS text) || '-01-01')::timestamp
          AND t.transfer_time < (CAST($1 + 1 AS text) || '-01-01')::timestamp
        GROUP BY 1 ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(endYear))
        .map(rows -> {
          List<TransferStats.YearAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TransferStats.YearAmount.fromRow(r));
          return list;
        });
  }
}
