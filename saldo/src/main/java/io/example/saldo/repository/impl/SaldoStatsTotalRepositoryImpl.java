package io.example.saldo.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.saldo.model.SaldoStats;
import io.example.saldo.repository.SaldoStatsTotalRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import io.example.saldo.domain.requests.MonthTotalSaldoBalance;

public class SaldoStatsTotalRepositoryImpl implements SaldoStatsTotalRepository {
  private final Pool client;

  public SaldoStatsTotalRepositoryImpl(Pool client) {
    this.client = client;
  }

  @Override
  public Future<List<SaldoStats.MonthTotalBalance>> getMonthlyTotalSaldoBalance(MonthTotalSaldoBalance req) {
    int year = req.getYear();
    int month = req.getMonth();

    OffsetDateTime p1Start = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime p1End = p1Start.plusMonths(1).minusNanos(1);
    OffsetDateTime p2Start = p1Start.minusMonths(1);
    OffsetDateTime p2End = p1Start.minusNanos(1);

    String sql = """
        WITH monthly_data AS (
            SELECT EXTRACT(YEAR FROM s.created_at)::integer AS year, EXTRACT(MONTH FROM s.created_at)::integer AS month, COALESCE(SUM(s.total_balance), 0) AS total_balance
            FROM saldos s WHERE s.deleted_at IS NULL
              AND ((s.created_at >= $1::timestamp AND s.created_at <= $2::timestamp) OR (s.created_at >= $3::timestamp AND s.created_at <= $4::timestamp))
            GROUP BY EXTRACT(YEAR FROM s.created_at), EXTRACT(MONTH FROM s.created_at)
        ), formatted_data AS (
            SELECT year::text, TO_CHAR(TO_DATE(month::text, 'MM'), 'Mon') AS month, total_balance::integer FROM monthly_data
            UNION ALL
            SELECT EXTRACT(YEAR FROM $1::timestamp)::text AS year, TO_CHAR($1::timestamp, 'Mon') AS month, 0::integer AS total_balance
            WHERE NOT EXISTS (SELECT 1 FROM monthly_data WHERE year = EXTRACT(YEAR FROM $1::timestamp)::integer AND month = EXTRACT(MONTH FROM $1::timestamp)::integer)
            UNION ALL
            SELECT EXTRACT(YEAR FROM $3::timestamp)::text AS year, TO_CHAR($3::timestamp, 'Mon') AS month, 0::integer AS total_balance
            WHERE NOT EXISTS (SELECT 1 FROM monthly_data WHERE year = EXTRACT(YEAR FROM $3::timestamp)::integer AND month = EXTRACT(MONTH FROM $3::timestamp)::integer)
        )
        SELECT * FROM formatted_data ORDER BY year DESC, TO_DATE(month, 'Mon') DESC
        """;

    return client.preparedQuery(sql)
        .execute(Tuple.of(p1Start, p1End, p2Start, p2End))
        .map(rows -> {
          List<SaldoStats.MonthTotalBalance> list = new ArrayList<>();
          for (Row r : rows)
            list.add(SaldoStats.MonthTotalBalance.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<SaldoStats.YearTotalBalance>> getYearlyTotalSaldoBalances(Integer currentYear) {
    String sql = """
        WITH yearly_data AS (
            SELECT EXTRACT(YEAR FROM s.created_at)::integer AS year, COALESCE(SUM(s.total_balance), 0)::integer AS total_balance
            FROM saldos s WHERE s.deleted_at IS NULL AND (EXTRACT(YEAR FROM s.created_at) = $1::integer OR EXTRACT(YEAR FROM s.created_at) = $1::integer - 1)
            GROUP BY EXTRACT(YEAR FROM s.created_at)
        ), formatted_data AS (
            SELECT year::text, total_balance::integer FROM yearly_data
            UNION ALL
            SELECT $1::text AS year, 0::integer AS total_balance WHERE NOT EXISTS (SELECT 1 FROM yearly_data WHERE year = $1::integer)
            UNION ALL
            SELECT ($1::integer - 1)::text AS year, 0::integer AS total_balance WHERE NOT EXISTS (SELECT 1 FROM yearly_data WHERE year = $1::integer - 1)
        )
        SELECT * FROM formatted_data ORDER BY year DESC
        """;

    return client.preparedQuery(sql)
        .execute(Tuple.of(currentYear))
        .map(rows -> {
          List<SaldoStats.YearTotalBalance> list = new ArrayList<>();
          for (Row r : rows)
            list.add(SaldoStats.YearTotalBalance.fromRow(r));
          return list;
        });
  }
}
