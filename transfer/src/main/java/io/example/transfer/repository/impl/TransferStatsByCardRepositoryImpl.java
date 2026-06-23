package io.example.transfer.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.transfer.domain.requests.MonthStatusTransferCardNumber;
import io.example.transfer.domain.requests.MonthYearCardNumber;
import io.example.transfer.domain.requests.YearStatusTransferCardNumber;
import io.example.transfer.model.TransferStats;
import io.example.transfer.repository.TransferStatsByCardAmountReceiverRepository;
import io.example.transfer.repository.TransferStatsByCardAmountSenderRepository;
import io.example.transfer.repository.TransferStatsByCardStatusRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransferStatsByCardRepositoryImpl implements
    TransferStatsByCardAmountSenderRepository,
    TransferStatsByCardAmountReceiverRepository,
    TransferStatsByCardStatusRepository {

  private final Pool pool;

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  @Override
  public Future<List<TransferStats.MonthAmount>> getMonthlySenderAmountsByCard(MonthYearCardNumber req) {
    String sql = """
        WITH months AS (
            SELECT generate_series(date_trunc('year', $2::timestamp), date_trunc('year', $2::timestamp) + interval '11 months', interval '1 month') AS month
        )
        SELECT TO_CHAR(m.month, 'Mon') AS month, COALESCE(SUM(t.transfer_amount), 0)::bigint AS total_transfer_amount
        FROM months m
        LEFT JOIN transfers t ON t.transfer_time >= m.month
            AND t.transfer_time < m.month + interval '1 month'
            AND t.transfer_from = $1 AND t.deleted_at IS NULL
        GROUP BY m.month ORDER BY m.month
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(req.getCardNumber(), getYearStart(req.getYear())))
        .map(rows -> {
          List<TransferStats.MonthAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TransferStats.MonthAmount.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<TransferStats.MonthAmount>> getMonthlyReceiverAmountsByCard(MonthYearCardNumber req) {
    String sql = """
        WITH months AS (
            SELECT generate_series(date_trunc('year', $2::timestamp), date_trunc('year', $2::timestamp) + interval '11 months', interval '1 month') AS month
        )
        SELECT TO_CHAR(m.month, 'Mon') AS month, COALESCE(SUM(t.transfer_amount), 0)::bigint AS total_transfer_amount
        FROM months m
        LEFT JOIN transfers t ON t.transfer_time >= m.month
            AND t.transfer_time < m.month + interval '1 month'
            AND t.transfer_to = $1 AND t.deleted_at IS NULL
        GROUP BY m.month ORDER BY m.month
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(req.getCardNumber(), getYearStart(req.getYear())))
        .map(rows -> {
          List<TransferStats.MonthAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TransferStats.MonthAmount.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<TransferStats.YearAmount>> getYearlySenderAmountsByCard(MonthYearCardNumber req) {
    String sql = """
        SELECT EXTRACT(YEAR FROM t.transfer_time)::integer AS year, COALESCE(SUM(t.transfer_amount), 0)::bigint AS total_transfer_amount
        FROM transfers t WHERE t.deleted_at IS NULL AND t.transfer_from = $1
          AND t.transfer_time >= (CAST($2 - 4 AS text) || '-01-01')::timestamp
          AND t.transfer_time < (CAST($2 + 1 AS text) || '-01-01')::timestamp
        GROUP BY 1 ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(req.getCardNumber(), req.getYear()))
        .map(rows -> {
          List<TransferStats.YearAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TransferStats.YearAmount.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<TransferStats.YearAmount>> getYearlyReceiverAmountsByCard(MonthYearCardNumber req) {
    String sql = """
        SELECT EXTRACT(YEAR FROM t.transfer_time)::integer AS year, COALESCE(SUM(t.transfer_amount), 0)::bigint AS total_transfer_amount
        FROM transfers t WHERE t.deleted_at IS NULL AND t.transfer_to = $1
          AND t.transfer_time >= (CAST($2 - 4 AS text) || '-01-01')::timestamp
          AND t.transfer_time < (CAST($2 + 1 AS text) || '-01-01')::timestamp
        GROUP BY 1 ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(req.getCardNumber(), req.getYear()))
        .map(rows -> {
          List<TransferStats.YearAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TransferStats.YearAmount.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<TransferStats.MonthStatus>> getMonthlyStatusByCard(MonthStatusTransferCardNumber req) {
    String card = req.getCardNumber();
    int year = req.getYear();
    int month = req.getMonth();
    String status = req.getStatus();

    OffsetDateTime curr = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime prev = curr.minusMonths(1);
    OffsetDateTime endCurr = curr.plusMonths(1).minusDays(1);
    OffsetDateTime endPrev = prev.plusMonths(1).minusDays(1);
    String countCol = "total_" + status;

    String sql = String.format(
        """
            WITH periods (st, et) AS (
                VALUES ($3::timestamp, $4::timestamp), ($5::timestamp, $6::timestamp)
            ),
            monthly_data AS (
                SELECT EXTRACT(YEAR FROM t.transfer_time)::integer AS yr, EXTRACT(MONTH FROM t.transfer_time)::integer AS mo,
                       COUNT(*) AS cnt, COALESCE(SUM(t.transfer_amount), 0)::bigint AS amt
                FROM transfers t WHERE t.deleted_at IS NULL AND t.status = $1 AND (t.transfer_from = $2 OR t.transfer_to = $2)
                  AND ((t.transfer_time >= $3::timestamp AND t.transfer_time <= $4::timestamp) OR (t.transfer_time >= $5::timestamp AND t.transfer_time <= $6::timestamp))
                GROUP BY 1, 2
            )
            SELECT EXTRACT(YEAR FROM p.st)::text AS year, TO_CHAR(p.st, 'Mon') AS month,
                   COALESCE(m.cnt, 0)::integer AS %s, COALESCE(m.amt, 0)::bigint AS total_amount
            FROM periods p
            LEFT JOIN monthly_data m ON m.yr = EXTRACT(YEAR FROM p.st)::integer AND m.mo = EXTRACT(MONTH FROM p.st)::integer
            ORDER BY year DESC, TO_DATE(month, 'Mon') DESC
            """,
        countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, card, curr, endCurr, prev, endPrev))
        .map(rows -> {
          List<TransferStats.MonthStatus> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TransferStats.MonthStatus.fromRow(r, countCol));
          return list;
        });
  }

  @Override
  public Future<List<TransferStats.YearStatus>> getYearlyStatusByCard(YearStatusTransferCardNumber req) {
    String card = req.getCardNumber();
    int year = req.getYear();
    String status = req.getStatus();
    String countCol = "total_" + status;

    String sql = String.format(
        """
            WITH years (yr) AS (
                VALUES ($3::integer), ($3::integer - 1)
            ),
            yearly_data AS (
                SELECT EXTRACT(YEAR FROM t.transfer_time)::integer AS year, COUNT(*) AS cnt, COALESCE(SUM(t.transfer_amount), 0)::bigint AS amt
                FROM transfers t WHERE t.deleted_at IS NULL AND t.status = $1 AND (t.transfer_from = $2 OR t.transfer_to = $2)
                  AND t.transfer_time >= (CAST($3 - 1 AS text) || '-01-01')::timestamp
                  AND t.transfer_time < (CAST($3 + 1 AS text) || '-01-01')::timestamp
                GROUP BY 1
            )
            SELECT y.yr::text AS year, COALESCE(d.cnt, 0)::bigint AS %s, COALESCE(d.amt, 0)::bigint AS total_amount
            FROM years y
            LEFT JOIN yearly_data d ON d.year = y.yr
            ORDER BY year DESC
            """,
        countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, card, year))
        .map(rows -> {
          List<TransferStats.YearStatus> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TransferStats.YearStatus.fromRow(r, countCol));
          return list;
        });
  }
}