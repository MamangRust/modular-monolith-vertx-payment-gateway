package io.example.transaction.repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.transaction.model.TransactionStats;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class TransactionStatsRepository {
  private final Pool pool;

  public TransactionStatsRepository(Pool pool) {
    this.pool = pool;
  }

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  // == GLOBAL ==
  public Future<List<TransactionStats.MonthAmount>> getMonthlyAmounts(int year) {
    String sql = """
        WITH mns AS (
          SELECT generate_series(
            date_trunc('year', $1::timestamp),
            date_trunc('year', $1::timestamp) + interval '1 year' - interval '1 day',
            interval '1 month'
          ) AS month
        )
        SELECT
            TO_CHAR(m.month, 'Mon') AS month,
            COALESCE(SUM(t.amount), 0)::integer AS total_amount
        FROM mns m
        LEFT JOIN transactions t ON EXTRACT(MONTH FROM t.transaction_time) = EXTRACT(MONTH FROM m.month)
          AND EXTRACT(YEAR FROM t.transaction_time) = EXTRACT(YEAR FROM m.month)
          AND t.deleted_at IS NULL
        GROUP BY m.month
        ORDER BY m.month
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(getYearStart(year)))
        .map(rows -> {
          List<TransactionStats.MonthAmount> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.MonthAmount.fromRow(r));
          return l;
        });
  }

  public Future<List<TransactionStats.YearAmount>> getYearlyAmounts(int endYear) {
    String sql = """
        SELECT
            EXTRACT(YEAR FROM t.created_at)::integer AS year,
            COALESCE(SUM(t.amount), 0)::integer AS total_amount
        FROM transactions t
        WHERE t.deleted_at IS NULL
          AND EXTRACT(YEAR FROM t.created_at) >= $1 - 4
          AND EXTRACT(YEAR FROM t.created_at) <= $1
        GROUP BY EXTRACT(YEAR FROM t.created_at)
        ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(endYear))
        .map(rows -> {
          List<TransactionStats.YearAmount> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.YearAmount.fromRow(r));
          return l;
        });
  }

  public Future<List<TransactionStats.MonthStatus>> getMonthlyStatus(int year, int month, String status) {
    OffsetDateTime curr = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime prev = curr.minusMonths(1);
    OffsetDateTime endCurr = curr.plusMonths(1).minusNanos(1);
    OffsetDateTime endPrev = curr.minusNanos(1);
    String countCol = "total_" + status;

    String sql = String.format("""
        WITH md AS (
            SELECT
                EXTRACT(YEAR FROM t.transaction_time)::integer AS year,
                EXTRACT(MONTH FROM t.transaction_time)::integer AS month,
                COUNT(*) AS %s,
                COALESCE(SUM(t.amount), 0)::integer AS total_amount
            FROM transactions t
            WHERE t.deleted_at IS NULL AND t.status = $1
              AND (
                (t.transaction_time >= $2::timestamp AND t.transaction_time <= $3::timestamp)
                OR (t.transaction_time >= $4::timestamp AND t.transaction_time <= $5::timestamp)
              )
            GROUP BY
                EXTRACT(YEAR FROM t.transaction_time),
                EXTRACT(MONTH FROM t.transaction_time)
        ), fd AS (
            SELECT
                year::text,
                TO_CHAR(TO_DATE(month::text, 'MM'), 'Mon') AS month,
                %s,
                total_amount
            FROM md
            UNION ALL
            SELECT
                EXTRACT(YEAR FROM $2::timestamp)::text AS year,
                TO_CHAR($2::timestamp, 'Mon') AS month,
                0 AS %s,
                0 AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM md
                WHERE year = EXTRACT(YEAR FROM $2::timestamp)::integer
                  AND month = EXTRACT(MONTH FROM $2::timestamp)::integer
            )
            UNION ALL
            SELECT
                EXTRACT(YEAR FROM $4::timestamp)::text AS year,
                TO_CHAR($4::timestamp, 'Mon') AS month,
                0 AS %s,
                0 AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM md
                WHERE year = EXTRACT(YEAR FROM $4::timestamp)::integer
                  AND month = EXTRACT(MONTH FROM $4::timestamp)::integer
            )
        )
        SELECT * FROM fd ORDER BY year DESC, TO_DATE(month, 'Mon') DESC
        """, countCol, countCol, countCol, countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, curr, endCurr, prev, endPrev))
        .map(rows -> {
          List<TransactionStats.MonthStatus> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.MonthStatus.fromRow(r, countCol));
          return l;
        });
  }

  public Future<List<TransactionStats.YearStatus>> getYearlyStatus(int endYear, String status) {
    String countCol = "total_" + status;
    String sql = String.format("""
        WITH yd AS (
            SELECT
                EXTRACT(YEAR FROM t.transaction_time)::integer AS year,
                COUNT(*) AS %s,
                COALESCE(SUM(t.amount), 0)::integer AS total_amount
            FROM transactions t
            WHERE t.deleted_at IS NULL AND t.status = $1
              AND (
                EXTRACT(YEAR FROM t.transaction_time) = $2::integer
                OR EXTRACT(YEAR FROM t.transaction_time) = $2::integer - 1
              )
            GROUP BY EXTRACT(YEAR FROM t.transaction_time)
        ), fd AS (
            SELECT
                year::text,
                %s::integer,
                total_amount
            FROM yd
            UNION ALL
            SELECT
                $2::text AS year,
                0::integer AS %s,
                0::integer AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM yd WHERE year = $2::integer
            )
            UNION ALL
            SELECT
                ($2::integer - 1)::text AS year,
                0::integer AS %s,
                0::integer AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM yd WHERE year = $2::integer - 1
            )
        )
        SELECT * FROM fd ORDER BY year DESC
        """, countCol, countCol, countCol, countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, endYear))
        .map(rows -> {
          List<TransactionStats.YearStatus> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.YearStatus.fromRow(r, countCol));
          return l;
        });
  }

  public Future<List<TransactionStats.MonthMethod>> getMonthlyMethods(int year) {
    String sql = """
        WITH mns AS (
          SELECT generate_series(
            date_trunc('year', $1::timestamp),
            date_trunc('year', $1::timestamp) + interval '1 year' - interval '1 day',
            interval '1 month'
          ) AS month
        ), pms AS (
          SELECT DISTINCT payment_method FROM transactions WHERE deleted_at IS NULL
        )
        SELECT
            TO_CHAR(m.month, 'Mon') AS month,
            pm.payment_method,
            COALESCE(COUNT(t.transaction_id), 0)::integer AS total_transactions,
            COALESCE(SUM(t.amount), 0)::integer AS total_amount
        FROM mns m
        CROSS JOIN pms pm
        LEFT JOIN transactions t ON EXTRACT(MONTH FROM t.transaction_time) = EXTRACT(MONTH FROM m.month)
          AND EXTRACT(YEAR FROM t.transaction_time) = EXTRACT(YEAR FROM m.month)
          AND t.payment_method = pm.payment_method
          AND t.deleted_at IS NULL
        GROUP BY m.month, pm.payment_method
        ORDER BY m.month, pm.payment_method
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(getYearStart(year)))
        .map(rows -> {
          List<TransactionStats.MonthMethod> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.MonthMethod.fromRow(r));
          return l;
        });
  }

  public Future<List<TransactionStats.YearMethod>> getYearlyMethods(int endYear) {
    String sql = """
        SELECT
            EXTRACT(YEAR FROM t.created_at)::integer AS year,
            t.payment_method,
            COUNT(t.transaction_id)::integer AS total_transactions,
            COALESCE(SUM(t.amount), 0)::integer AS total_amount
        FROM transactions t
        WHERE t.deleted_at IS NULL
          AND EXTRACT(YEAR FROM t.created_at) >= $1 - 4
          AND EXTRACT(YEAR FROM t.created_at) <= $1
        GROUP BY EXTRACT(YEAR FROM t.created_at), t.payment_method
        ORDER BY year, t.payment_method
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(endYear))
        .map(rows -> {
          List<TransactionStats.YearMethod> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.YearMethod.fromRow(r));
          return l;
        });
  }

  // == BY CARD ==
  public Future<List<TransactionStats.MonthAmount>> getMonthlyAmountsByCard(String card, int year) {
    String sql = """
        WITH mns AS (
          SELECT generate_series(
            date_trunc('year', $2::timestamp),
            date_trunc('year', $2::timestamp) + interval '1 year' - interval '1 day',
            interval '1 month'
          ) AS month
        )
        SELECT
            TO_CHAR(m.month, 'Mon') AS month,
            COALESCE(SUM(t.amount), 0)::integer AS total_amount
        FROM mns m
        LEFT JOIN transactions t ON EXTRACT(MONTH FROM t.transaction_time) = EXTRACT(MONTH FROM m.month)
          AND EXTRACT(YEAR FROM t.transaction_time) = EXTRACT(YEAR FROM m.month)
          AND t.card_number = $1
          AND t.deleted_at IS NULL
        GROUP BY m.month
        ORDER BY m.month
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(card, getYearStart(year)))
        .map(rows -> {
          List<TransactionStats.MonthAmount> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.MonthAmount.fromRow(r));
          return l;
        });
  }

  public Future<List<TransactionStats.YearAmount>> getYearlyAmountsByCard(String card, int endYear) {
    String sql = """
        SELECT
            EXTRACT(YEAR FROM t.created_at)::integer AS year,
            COALESCE(SUM(t.amount), 0)::integer AS total_amount
        FROM transactions t
        WHERE t.card_number = $1
          AND t.deleted_at IS NULL
          AND EXTRACT(YEAR FROM t.created_at) >= $2 - 4
          AND EXTRACT(YEAR FROM t.created_at) <= $2
        GROUP BY EXTRACT(YEAR FROM t.created_at)
        ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(card, endYear))
        .map(rows -> {
          List<TransactionStats.YearAmount> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.YearAmount.fromRow(r));
          return l;
        });
  }

  public Future<List<TransactionStats.MonthStatus>> getMonthlyStatusByCard(String card, int year, int month, String status) {
    OffsetDateTime curr = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime prev = curr.minusMonths(1);
    OffsetDateTime endCurr = curr.plusMonths(1).minusNanos(1);
    OffsetDateTime endPrev = curr.minusNanos(1);
    String countCol = "total_" + status;

    String sql = String.format("""
        WITH md AS (
            SELECT
                EXTRACT(YEAR FROM t.transaction_time)::integer AS year,
                EXTRACT(MONTH FROM t.transaction_time)::integer AS month,
                COUNT(*) AS %s,
                COALESCE(SUM(t.amount), 0)::integer AS total_amount
            FROM transactions t
            WHERE t.deleted_at IS NULL AND t.status = $1 AND t.card_number = $2
              AND (
                (t.transaction_time >= $3::timestamp AND t.transaction_time <= $4::timestamp)
                OR (t.transaction_time >= $5::timestamp AND t.transaction_time <= $6::timestamp)
              )
            GROUP BY
                EXTRACT(YEAR FROM t.transaction_time),
                EXTRACT(MONTH FROM t.transaction_time)
        ), fd AS (
            SELECT
                year::text,
                TO_CHAR(TO_DATE(month::text, 'MM'), 'Mon') AS month,
                %s,
                total_amount
            FROM md
            UNION ALL
            SELECT
                EXTRACT(YEAR FROM $3::timestamp)::text AS year,
                TO_CHAR($3::timestamp, 'Mon') AS month,
                0 AS %s,
                0 AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM md
                WHERE year = EXTRACT(YEAR FROM $3::timestamp)::integer
                  AND month = EXTRACT(MONTH FROM $3::timestamp)::integer
            )
            UNION ALL
            SELECT
                EXTRACT(YEAR FROM $5::timestamp)::text AS year,
                TO_CHAR($5::timestamp, 'Mon') AS month,
                0 AS %s,
                0 AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM md
                WHERE year = EXTRACT(YEAR FROM $5::timestamp)::integer
                  AND month = EXTRACT(MONTH FROM $5::timestamp)::integer
            )
        )
        SELECT * FROM fd ORDER BY year DESC, TO_DATE(month, 'Mon') DESC
        """, countCol, countCol, countCol, countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, card, curr, endCurr, prev, endPrev))
        .map(rows -> {
          List<TransactionStats.MonthStatus> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.MonthStatus.fromRow(r, countCol));
          return l;
        });
  }

  public Future<List<TransactionStats.YearStatus>> getYearlyStatusByCard(String card, int endYear, String status) {
    String countCol = "total_" + status;
    String sql = String.format("""
        WITH yd AS (
            SELECT
                EXTRACT(YEAR FROM t.transaction_time)::integer AS year,
                COUNT(*) AS %s,
                COALESCE(SUM(t.amount), 0)::integer AS total_amount
            FROM transactions t
            WHERE t.deleted_at IS NULL AND t.status = $1 AND t.card_number = $2
              AND (
                EXTRACT(YEAR FROM t.transaction_time) = $3::integer
                OR EXTRACT(YEAR FROM t.transaction_time) = $3::integer - 1
              )
            GROUP BY EXTRACT(YEAR FROM t.transaction_time)
        ), fd AS (
            SELECT
                year::text,
                %s::integer,
                total_amount
            FROM yd
            UNION ALL
            SELECT
                $3::text AS year,
                0::integer AS %s,
                0::integer AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM yd WHERE year = $3::integer
            )
            UNION ALL
            SELECT
                ($3::integer - 1)::text AS year,
                0::integer AS %s,
                0::integer AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM yd WHERE year = $3::integer - 1
            )
        )
        SELECT * FROM fd ORDER BY year DESC
        """, countCol, countCol, countCol, countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, card, endYear))
        .map(rows -> {
          List<TransactionStats.YearStatus> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.YearStatus.fromRow(r, countCol));
          return l;
        });
  }

  public Future<List<TransactionStats.MonthMethod>> getMonthlyMethodsByCard(String card, int year) {
    String sql = """
        WITH mns AS (
          SELECT generate_series(
            date_trunc('year', $2::timestamp),
            date_trunc('year', $2::timestamp) + interval '1 year' - interval '1 day',
            interval '1 month'
          ) AS month
        ), pms AS (
          SELECT DISTINCT payment_method FROM transactions WHERE deleted_at IS NULL
        )
        SELECT
            TO_CHAR(m.month, 'Mon') AS month,
            pm.payment_method,
            COALESCE(COUNT(t.transaction_id), 0)::integer AS total_transactions,
            COALESCE(SUM(t.amount), 0)::integer AS total_amount
        FROM mns m
        CROSS JOIN pms pm
        LEFT JOIN transactions t ON EXTRACT(MONTH FROM t.transaction_time) = EXTRACT(MONTH FROM m.month)
          AND EXTRACT(YEAR FROM t.transaction_time) = EXTRACT(YEAR FROM m.month)
          AND t.payment_method = pm.payment_method
          AND t.card_number = $1
          AND t.deleted_at IS NULL
        GROUP BY m.month, pm.payment_method
        ORDER BY m.month, pm.payment_method
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(card, getYearStart(year)))
        .map(rows -> {
          List<TransactionStats.MonthMethod> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.MonthMethod.fromRow(r));
          return l;
        });
  }

  public Future<List<TransactionStats.YearMethod>> getYearlyMethodsByCard(String card, int endYear) {
    String sql = """
        SELECT
            EXTRACT(YEAR FROM t.created_at)::integer AS year,
            t.payment_method,
            COUNT(t.transaction_id)::integer AS total_transactions,
            COALESCE(SUM(t.amount), 0)::integer AS total_amount
        FROM transactions t
        WHERE t.card_number = $1
          AND t.deleted_at IS NULL
          AND EXTRACT(YEAR FROM t.created_at) >= $2 - 4
          AND EXTRACT(YEAR FROM t.created_at) <= $2
        GROUP BY EXTRACT(YEAR FROM t.created_at), t.payment_method
        ORDER BY year, t.payment_method
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(card, endYear))
        .map(rows -> {
          List<TransactionStats.YearMethod> l = new ArrayList<>();
          for (Row r : rows) l.add(TransactionStats.YearMethod.fromRow(r));
          return l;
        });
  }
}
