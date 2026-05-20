package io.example.topup.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.topup.model.TopupStats;
import io.example.topup.repository.TopupStatsAmountRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import pb.topup.Topup.FindYearTopupStatus;

public class TopupStatsAmountRepositoryImpl implements TopupStatsAmountRepository {
  private final Pool pool;

  public TopupStatsAmountRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  @Override
  public Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmounts(FindYearTopupStatus req) {
    int year = req.getYear();
    String sql = """
        WITH months AS (
            SELECT generate_series(date_trunc('year', $1::timestamp), date_trunc('year', $1::timestamp) + interval '1 year' - interval '1 day', interval '1 month') AS month
        )
        SELECT TO_CHAR(m.month, 'Mon') AS month, COALESCE(SUM(t.topup_amount), 0)::integer AS total_amount
        FROM months m
        LEFT JOIN topups t ON EXTRACT(MONTH FROM t.topup_time) = EXTRACT(MONTH FROM m.month)
            AND EXTRACT(YEAR FROM t.topup_time) = EXTRACT(YEAR FROM m.month) AND t.deleted_at IS NULL
        GROUP BY m.month ORDER BY m.month
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(getYearStart(year)))
        .map(rows -> {
          List<TopupStats.MonthAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TopupStats.MonthAmount.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<TopupStats.YearAmount>> getYearlyTopupAmounts(FindYearTopupStatus req) {
    int endYear = req.getYear();
    String sql = """
        WITH years AS (
            SELECT generate_series($1::integer - 4, $1::integer) AS year
        )
        SELECT y.year, COALESCE(SUM(t.topup_amount), 0)::integer AS total_amount
        FROM years y
        LEFT JOIN topups t ON EXTRACT(YEAR FROM t.topup_time) = y.year AND t.deleted_at IS NULL
        GROUP BY y.year ORDER BY y.year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(endYear))
        .map(rows -> {
          List<TopupStats.YearAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TopupStats.YearAmount.fromRow(r));
          return list;
        });
  }
}
