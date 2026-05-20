package io.example.transaction.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import io.example.transaction.model.TransactionStats;
import io.example.transaction.repository.TransactionStatsStatusRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import pb.transaction.Transaction.FindMonthlyTransactionStatus;
import pb.transaction.Transaction.FindYearTransactionStatus;
import pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber;
import pb.transaction.Transaction.FindYearTransactionStatusCardNumber;

public class TransactionStatsStatusRepositoryImpl implements TransactionStatsStatusRepository {
  private final Pool pool;

  public TransactionStatsStatusRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  @Override
  public Future<List<TransactionStats.MonthStatus>> getMonthlyStatus(FindMonthlyTransactionStatus req, String status) {
    int year = (int) req.getYear();
    int month = (int) req.getMonth();
    OffsetDateTime curr = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime prev = curr.minusMonths(1);
    OffsetDateTime endCurr = curr.plusMonths(1).minusNanos(1);
    OffsetDateTime endPrev = curr.minusNanos(1);
    String countCol = "total_" + status;

    String sql = String.format("""
        WITH monthly_data AS (
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
        ), formatted_data AS (
            SELECT
                year::text,
                TO_CHAR(TO_DATE(month::text, 'MM'), 'Mon') AS month,
                %s,
                total_amount
            FROM monthly_data
            UNION ALL
            SELECT
                EXTRACT(YEAR FROM $2::timestamp)::text AS year,
                TO_CHAR($2::timestamp, 'Mon') AS month,
                0 AS %s,
                0 AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM monthly_data
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
                SELECT 1 FROM monthly_data
                WHERE year = EXTRACT(YEAR FROM $4::timestamp)::integer
                  AND month = EXTRACT(MONTH FROM $4::timestamp)::integer
            )
        )
        SELECT * FROM formatted_data ORDER BY year DESC, TO_DATE(month, 'Mon') DESC
        """, countCol, countCol, countCol, countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, curr, endCurr, prev, endPrev))
        .map(rows -> {
          List<TransactionStats.MonthStatus> list = new ArrayList<>();
          for (Row r : rows) list.add(TransactionStats.MonthStatus.fromRow(r, countCol));
          return list;
        });
  }

  @Override
  public Future<List<TransactionStats.YearStatus>> getYearlyStatus(FindYearTransactionStatus req, String status) {
    int endYear = (int) req.getYear();
    String countCol = "total_" + status;
    String sql = String.format("""
        WITH yearly_data AS (
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
        ), formatted_data AS (
            SELECT
                year::text,
                %s::integer,
                total_amount
            FROM yearly_data
            UNION ALL
            SELECT
                $2::text AS year,
                0::integer AS %s,
                0::integer AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM yearly_data WHERE year = $2::integer
            )
            UNION ALL
            SELECT
                ($2::integer - 1)::text AS year,
                0::integer AS %s,
                0::integer AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM yearly_data WHERE year = $2::integer - 1
            )
        )
        SELECT * FROM formatted_data ORDER BY year DESC
        """, countCol, countCol, countCol, countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, endYear))
        .map(rows -> {
          List<TransactionStats.YearStatus> list = new ArrayList<>();
          for (Row r : rows) list.add(TransactionStats.YearStatus.fromRow(r, countCol));
          return list;
        });
  }

  @Override
  public Future<List<TransactionStats.MonthStatus>> getMonthlyStatusByCard(FindMonthlyTransactionStatusCardNumber req, String status) {
    int year = (int) req.getYear();
    int month = (int) req.getMonth();
    String card = req.getCardNumber();
    OffsetDateTime curr = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime prev = curr.minusMonths(1);
    OffsetDateTime endCurr = curr.plusMonths(1).minusNanos(1);
    OffsetDateTime endPrev = curr.minusNanos(1);
    String countCol = "total_" + status;

    String sql = String.format("""
        WITH monthly_data AS (
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
        ), formatted_data AS (
            SELECT
                year::text,
                TO_CHAR(TO_DATE(month::text, 'MM'), 'Mon') AS month,
                %s,
                total_amount
            FROM monthly_data
            UNION ALL
            SELECT
                EXTRACT(YEAR FROM $3::timestamp)::text AS year,
                TO_CHAR($3::timestamp, 'Mon') AS month,
                0 AS %s,
                0 AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM monthly_data
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
                SELECT 1 FROM monthly_data
                WHERE year = EXTRACT(YEAR FROM $5::timestamp)::integer
                  AND month = EXTRACT(MONTH FROM $5::timestamp)::integer
            )
        )
        SELECT * FROM formatted_data ORDER BY year DESC, TO_DATE(month, 'Mon') DESC
        """, countCol, countCol, countCol, countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, card, curr, endCurr, prev, endPrev))
        .map(rows -> {
          List<TransactionStats.MonthStatus> list = new ArrayList<>();
          for (Row r : rows) list.add(TransactionStats.MonthStatus.fromRow(r, countCol));
          return list;
        });
  }

  @Override
  public Future<List<TransactionStats.YearStatus>> getYearlyStatusByCard(FindYearTransactionStatusCardNumber req, String status) {
    int year = (int) req.getYear();
    String card = req.getCardNumber();
    String countCol = "total_" + status;
    String sql = String.format("""
        WITH yearly_data AS (
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
        ), formatted_data AS (
            SELECT
                year::text,
                %s::integer,
                total_amount
            FROM yearly_data
            UNION ALL
            SELECT
                $3::text AS year,
                0::integer AS %s,
                0::integer AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM yearly_data WHERE year = $3::integer
            )
            UNION ALL
            SELECT
                ($3::integer - 1)::text AS year,
                0::integer AS %s,
                0::integer AS total_amount
            WHERE NOT EXISTS (
                SELECT 1 FROM yearly_data WHERE year = $3::integer - 1
            )
        )
        SELECT * FROM formatted_data ORDER BY year DESC
        """, countCol, countCol, countCol, countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, card, year))
        .map(rows -> {
          List<TransactionStats.YearStatus> list = new ArrayList<>();
          for (Row r : rows) list.add(TransactionStats.YearStatus.fromRow(r, countCol));
          return list;
        });
  }
}
