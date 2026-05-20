package io.example.saldo.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.saldo.model.SaldoStats;
import io.example.saldo.repository.SaldoStatsBalanceRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class SaldoStatsBalanceRepositoryImpl implements SaldoStatsBalanceRepository {
  private final Pool client;

  public SaldoStatsBalanceRepositoryImpl(Pool client) {
    this.client = client;
  }

  @Override
  public Future<List<SaldoStats.MonthBalance>> getMonthlySaldoBalances(Integer year) {
    OffsetDateTime yearStart = OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    String sql = """
        WITH months AS (
            SELECT generate_series(date_trunc('year', $1::timestamp), date_trunc('year', $1::timestamp) + interval '1 year' - interval '1 day', interval '1 month') AS month
        )
        SELECT TO_CHAR(m.month, 'Mon') AS month, COALESCE(SUM(s.total_balance), 0)::bigint AS total_balance
        FROM months m
        LEFT JOIN saldos s ON EXTRACT(MONTH FROM s.created_at) = EXTRACT(MONTH FROM m.month) AND EXTRACT(YEAR FROM s.created_at) = EXTRACT(YEAR FROM m.month) AND s.deleted_at IS NULL
        GROUP BY m.month ORDER BY m.month
        """;

    return client.preparedQuery(sql)
        .execute(Tuple.of(yearStart))
        .map(rows -> {
          List<SaldoStats.MonthBalance> list = new ArrayList<>();
          for (Row r : rows)
            list.add(SaldoStats.MonthBalance.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<SaldoStats.YearBalance>> getYearlySaldoBalances(Integer endYear) {
    String sql = """
        WITH years AS (
            SELECT generate_series($1::int - 4, $1::int) AS year
        ),
        yearly_data AS (
            SELECT EXTRACT(YEAR FROM s.created_at)::int AS year, SUM(s.total_balance) AS total_balance
            FROM saldos s
            WHERE s.deleted_at IS NULL
              AND EXTRACT(YEAR FROM s.created_at) >= $1::int - 4
              AND EXTRACT(YEAR FROM s.created_at) <= $1::int
            GROUP BY EXTRACT(YEAR FROM s.created_at)
        )
        SELECT y.year::text, COALESCE(yd.total_balance, 0)::bigint AS total_balance
        FROM years y
        LEFT JOIN yearly_data yd ON y.year = yd.year
        ORDER BY y.year
        """;

    return client.preparedQuery(sql)
        .execute(Tuple.of(endYear))
        .map(rows -> {
          List<SaldoStats.YearBalance> list = new ArrayList<>();
          for (Row r : rows)
            list.add(SaldoStats.YearBalance.fromRow(r));
          return list;
        });
  }
}
