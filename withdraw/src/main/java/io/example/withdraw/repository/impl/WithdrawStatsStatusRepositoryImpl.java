package io.example.withdraw.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.withdraw.domain.requests.MonthStatusWithdrawCardNumber;
import io.example.withdraw.domain.requests.YearStatusWithdrawCardNumber;
import io.example.withdraw.model.WithdrawStats;
import io.example.withdraw.repository.WithdrawStatsStatusRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WithdrawStatsStatusRepositoryImpl implements WithdrawStatsStatusRepository {
  private final Pool pool;

  @Override
  public Future<List<WithdrawStats.MonthStatus>> getMonthlyWithdrawStatus(MonthStatusWithdrawCardNumber req) {
    OffsetDateTime curr = OffsetDateTime.of(req.getYear(), req.getMonth(), 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime prev = curr.minusMonths(1);
    OffsetDateTime endCurr = curr.plusMonths(1).minusDays(1);
    OffsetDateTime endPrev = prev.plusMonths(1).minusDays(1);
    String countCol = "total_" + req.getStatus();

    String sql = String.format(
        """
            WITH periods (st, et) AS (
                VALUES ($2::timestamp, $3::timestamp), ($4::timestamp, $5::timestamp)
            ),
            monthly_data AS (
                SELECT EXTRACT(YEAR FROM w.withdraw_time)::integer AS yr, EXTRACT(MONTH FROM w.withdraw_time)::integer AS mo,
                       COUNT(*) AS cnt, COALESCE(SUM(w.withdraw_amount), 0)::bigint AS amt
                FROM withdraws w WHERE w.deleted_at IS NULL AND w.status = $1
                  AND ((w.withdraw_time >= $2::timestamp AND w.withdraw_time <= $3::timestamp) OR (w.withdraw_time >= $4::timestamp AND w.withdraw_time <= $5::timestamp))
                GROUP BY 1, 2
            )
            SELECT EXTRACT(YEAR FROM p.st)::text AS year, TO_CHAR(p.st, 'Mon') AS month,
                   COALESCE(m.cnt, 0)::integer AS %s, COALESCE(m.amt, 0)::bigint AS total_amount
            FROM periods p
            LEFT JOIN monthly_data m ON m.yr = EXTRACT(YEAR FROM p.st)::integer AND m.mo = EXTRACT(MONTH FROM p.st)::integer
            ORDER BY year DESC, p.st DESC
            """,
        countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(req.getStatus(), curr, endCurr, prev, endPrev))
        .map(rows -> {
          List<WithdrawStats.MonthStatus> list = new ArrayList<>();
          for (Row r : rows)
            list.add(WithdrawStats.MonthStatus.fromRow(r, countCol));
          return list;
        });
  }

  @Override
  public Future<List<WithdrawStats.YearStatus>> getYearlyWithdrawStatus(YearStatusWithdrawCardNumber req) {
    String countCol = "total_" + req.getStatus();
    String sql = String.format(
        """
            WITH years (yr) AS (
                VALUES ($2::integer), ($2::integer - 1)
            ),
            yearly_data AS (
                SELECT EXTRACT(YEAR FROM w.withdraw_time)::integer AS year, COUNT(*) AS cnt, COALESCE(SUM(w.withdraw_amount), 0)::bigint AS amt
                FROM withdraws w WHERE w.deleted_at IS NULL AND w.status = $1
                  AND w.withdraw_time >= (CAST($2 - 1 AS text) || '-01-01')::timestamp
                  AND w.withdraw_time < (CAST($2 + 1 AS text) || '-01-01')::timestamp
                GROUP BY 1
            )
            SELECT y.yr::text AS year, COALESCE(d.cnt, 0)::bigint AS %s, COALESCE(d.amt, 0)::bigint AS total_amount
            FROM years y
            LEFT JOIN yearly_data d ON d.year = y.yr
            ORDER BY year DESC
            """,
        countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(req.getStatus(), req.getYear()))
        .map(rows -> {
          List<WithdrawStats.YearStatus> list = new ArrayList<>();
          for (Row r : rows)
            list.add(WithdrawStats.YearStatus.fromRow(r, countCol));
          return list;
        });
  }

  @Override
  public Future<List<WithdrawStats.MonthStatus>> getMonthlyStatusByCard(MonthStatusWithdrawCardNumber req) {
    OffsetDateTime curr = OffsetDateTime.of(req.getYear(), req.getMonth(), 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime prev = curr.minusMonths(1);
    OffsetDateTime endCurr = curr.plusMonths(1).minusDays(1);
    OffsetDateTime endPrev = prev.plusMonths(1).minusDays(1);
    String countCol = "total_" + req.getStatus();

    String sql = String.format(
        """
            WITH periods (st, et) AS (
                VALUES ($3::timestamp, $4::timestamp), ($5::timestamp, $6::timestamp)
            ),
            monthly_data AS (
                SELECT EXTRACT(YEAR FROM w.withdraw_time)::integer AS yr, EXTRACT(MONTH FROM w.withdraw_time)::integer AS mo,
                       COUNT(*) AS cnt, COALESCE(SUM(w.withdraw_amount), 0)::bigint AS amt
                FROM withdraws w WHERE w.deleted_at IS NULL AND w.status = $1 AND w.card_number = $2
                  AND ((w.withdraw_time >= $3::timestamp AND w.withdraw_time <= $4::timestamp) OR (w.withdraw_time >= $5::timestamp AND w.withdraw_time <= $6::timestamp))
                GROUP BY 1, 2
            )
            SELECT EXTRACT(YEAR FROM p.st)::text AS year, TO_CHAR(p.st, 'Mon') AS month,
                   COALESCE(m.cnt, 0)::integer AS %s, COALESCE(m.amt, 0)::bigint AS total_amount
            FROM periods p
            LEFT JOIN monthly_data m ON m.yr = EXTRACT(YEAR FROM p.st)::integer AND m.mo = EXTRACT(MONTH FROM p.st)::integer
            ORDER BY year DESC, p.st DESC
            """,
        countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(req.getStatus(), req.getCardNumber(), curr, endCurr, prev, endPrev))
        .map(rows -> {
          List<WithdrawStats.MonthStatus> list = new ArrayList<>();
          for (Row r : rows)
            list.add(WithdrawStats.MonthStatus.fromRow(r, countCol));
          return list;
        });
  }

  @Override
  public Future<List<WithdrawStats.YearStatus>> getYearlyStatusByCard(YearStatusWithdrawCardNumber req) {
    String countCol = "total_" + req.getStatus();
    String sql = String.format(
        """
            WITH years (yr) AS (
                VALUES ($3::integer), ($3::integer - 1)
            ),
            yearly_data AS (
                SELECT EXTRACT(YEAR FROM w.withdraw_time)::integer AS year, COUNT(*) AS cnt, COALESCE(SUM(w.withdraw_amount), 0)::bigint AS amt
                FROM withdraws w WHERE w.deleted_at IS NULL AND w.status = $1 AND w.card_number = $2
                  AND w.withdraw_time >= (CAST($3 - 1 AS text) || '-01-01')::timestamp
                  AND w.withdraw_time < (CAST($3 + 1 AS text) || '-01-01')::timestamp
                GROUP BY 1
            )
            SELECT y.yr::text AS year, COALESCE(d.cnt, 0)::bigint AS %s, COALESCE(d.amt, 0)::bigint AS total_amount
            FROM years y
            LEFT JOIN yearly_data d ON d.year = y.yr
            ORDER BY year DESC
            """,
        countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(req.getStatus(), req.getCardNumber(), req.getYear()))
        .map(rows -> {
          List<WithdrawStats.YearStatus> list = new ArrayList<>();
          for (Row r : rows)
            list.add(WithdrawStats.YearStatus.fromRow(r, countCol));
          return list;
        });
  }
}
