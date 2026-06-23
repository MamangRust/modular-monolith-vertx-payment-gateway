package io.example.withdraw.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.withdraw.domain.requests.YearMonthCardNumber;
import io.example.withdraw.model.WithdrawStats;
import io.example.withdraw.repository.WithdrawStatsAmountRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WithdrawStatsAmountRepositoryImpl implements WithdrawStatsAmountRepository {
  private final Pool pool;

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  @Override
  public Future<List<WithdrawStats.MonthAmount>> getMonthlyWithdrawAmounts(int year) {
    String sql = """
        WITH months AS (
            SELECT generate_series(date_trunc('year', $1::timestamp), date_trunc('year', $1::timestamp) + interval '11 months', interval '1 month') AS month
        )
        SELECT TO_CHAR(m.month, 'Mon') AS month, COALESCE(SUM(w.withdraw_amount), 0)::bigint AS total_withdraw_amount
        FROM months m
        LEFT JOIN withdraws w ON w.withdraw_time >= m.month
            AND w.withdraw_time < m.month + interval '1 month'
            AND w.deleted_at IS NULL
        GROUP BY m.month ORDER BY m.month
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(getYearStart(year)))
        .map(rows -> {
          List<WithdrawStats.MonthAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(WithdrawStats.MonthAmount.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<WithdrawStats.YearAmount>> getYearlyWithdrawAmounts(int endYear) {
    String sql = """
        SELECT EXTRACT(YEAR FROM w.withdraw_time)::integer AS year, COALESCE(SUM(w.withdraw_amount), 0)::bigint AS total_withdraw_amount
        FROM withdraws w WHERE w.deleted_at IS NULL
          AND w.withdraw_time >= (CAST($1 - 4 AS text) || '-01-01')::timestamp
          AND w.withdraw_time < (CAST($1 + 1 AS text) || '-01-01')::timestamp
        GROUP BY 1 ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(endYear))
        .map(rows -> {
          List<WithdrawStats.YearAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(WithdrawStats.YearAmount.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<WithdrawStats.MonthAmount>> getMonthlyWithdrawAmountsByCard(YearMonthCardNumber req) {
    String sql = """
        WITH months AS (
            SELECT generate_series(date_trunc('year', $2::timestamp), date_trunc('year', $2::timestamp) + interval '11 months', interval '1 month') AS month
        )
        SELECT TO_CHAR(m.month, 'Mon') AS month, COALESCE(SUM(w.withdraw_amount), 0)::bigint AS total_withdraw_amount
        FROM months m
        LEFT JOIN withdraws w ON w.withdraw_time >= m.month
            AND w.withdraw_time < m.month + interval '1 month'
            AND w.card_number = $1 AND w.deleted_at IS NULL
        GROUP BY m.month ORDER BY m.month
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(req.getCardNumber(), getYearStart(req.getYear())))
        .map(rows -> {
          List<WithdrawStats.MonthAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(WithdrawStats.MonthAmount.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<WithdrawStats.YearAmount>> getYearlyWithdrawAmountsByCard(YearMonthCardNumber req) {
    String sql = """
        SELECT EXTRACT(YEAR FROM w.withdraw_time)::integer AS year, COALESCE(SUM(w.withdraw_amount), 0)::bigint AS total_withdraw_amount
        FROM withdraws w WHERE w.deleted_at IS NULL AND w.card_number = $1
          AND w.withdraw_time >= (CAST($2 - 4 AS text) || '-01-01')::timestamp
          AND w.withdraw_time < (CAST($2 + 1 AS text) || '-01-01')::timestamp
        GROUP BY 1 ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(req.getCardNumber(), req.getYear()))
        .map(rows -> {
          List<WithdrawStats.YearAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(WithdrawStats.YearAmount.fromRow(r));
          return list;
        });
  }
}
