package io.example.transfer.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.transfer.domain.requests.MonthStatusTransfer;
import io.example.transfer.domain.requests.YearStatusTransferRequest;
import io.example.transfer.model.TransferStats;
import io.example.transfer.repository.TransferStatsStatusRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransferStatsStatusRepositoryImpl implements TransferStatsStatusRepository {
  private final Pool pool;

  @Override
  public Future<List<TransferStats.MonthStatus>> getMonthlyTransferStatus(MonthStatusTransfer req) {
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
                VALUES ($2::timestamp, $3::timestamp), ($4::timestamp, $5::timestamp)
            ),
            monthly_data AS (
                SELECT EXTRACT(YEAR FROM t.transfer_time)::integer AS yr, EXTRACT(MONTH FROM t.transfer_time)::integer AS mo,
                       COUNT(*) AS cnt, COALESCE(SUM(t.transfer_amount), 0)::bigint AS amt
                FROM transfers t WHERE t.deleted_at IS NULL AND t.status = $1
                  AND ((t.transfer_time >= $2::timestamp AND t.transfer_time <= $3::timestamp) OR (t.transfer_time >= $4::timestamp AND t.transfer_time <= $5::timestamp))
                GROUP BY 1, 2
            )
            SELECT EXTRACT(YEAR FROM p.st)::text AS year, TO_CHAR(p.st, 'Mon') AS month,
                   COALESCE(m.cnt, 0)::integer AS %s, COALESCE(m.amt, 0)::bigint AS total_amount
            FROM periods p
            LEFT JOIN monthly_data m ON m.yr = EXTRACT(YEAR FROM p.st)::integer AND m.mo = EXTRACT(MONTH FROM p.st)::integer
            ORDER BY year DESC, TO_DATE(month, 'Mon') DESC
            """,
        countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, curr, endCurr, prev, endPrev))
        .map(rows -> {
          List<TransferStats.MonthStatus> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TransferStats.MonthStatus.fromRow(r, countCol));
          return list;
        });
  }

  @Override
  public Future<List<TransferStats.YearStatus>> getYearlyTransferStatus(YearStatusTransferRequest req) {
    int endYear = req.getYear();
    String status = req.getStatus();
    String countCol = "total_" + status;

    String sql = String.format(
        """
            WITH years (yr) AS (
                VALUES ($2::integer), ($2::integer - 1)
            ),
            yearly_data AS (
                SELECT EXTRACT(YEAR FROM t.transfer_time)::integer AS year, COUNT(*) AS cnt, COALESCE(SUM(t.transfer_amount), 0)::bigint AS amt
                FROM transfers t WHERE t.deleted_at IS NULL AND t.status = $1
                  AND t.transfer_time >= (CAST($2 - 1 AS text) || '-01-01')::timestamp
                  AND t.transfer_time < (CAST($2 + 1 AS text) || '-01-01')::timestamp
                GROUP BY 1
            )
            SELECT y.yr::text AS year, COALESCE(d.cnt, 0)::bigint AS %s, COALESCE(d.amt, 0)::bigint AS total_amount
            FROM years y
            LEFT JOIN yearly_data d ON d.year = y.yr
            ORDER BY year DESC
            """,
        countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(status, endYear))
        .map(rows -> {
          List<TransferStats.YearStatus> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TransferStats.YearStatus.fromRow(r, countCol));
          return list;
        });
  }
}