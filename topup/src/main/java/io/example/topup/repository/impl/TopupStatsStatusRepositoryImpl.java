package io.example.topup.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.topup.domain.requests.topup.MonthTopupStatusRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusRequest;
import io.example.topup.model.TopupStats;
import io.example.topup.repository.TopupStatsStatusRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TopupStatsStatusRepositoryImpl implements TopupStatsStatusRepository {
  private final Pool pool;

  @Override
  public Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatus(MonthTopupStatusRequest req) {
    int year = req.getYear();
    int month = req.getMonth();
    OffsetDateTime curr = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime prev = curr.minusMonths(1);
    OffsetDateTime endCurr = curr.plusMonths(1).minusNanos(1);
    OffsetDateTime endPrev = curr.minusNanos(1);
    String countCol = "total_" + req.getStatus();

    String sql = String.format(
        """
            WITH monthly_data AS (
                SELECT EXTRACT(YEAR FROM t.topup_time)::integer AS year, EXTRACT(MONTH FROM t.topup_time)::integer AS month,
                       COUNT(*) AS %s, COALESCE(SUM(t.topup_amount), 0)::integer AS total_amount
                FROM topups t WHERE t.deleted_at IS NULL AND t.status = $1
                  AND ((t.topup_time >= $2::timestamp AND t.topup_time <= $3::timestamp) OR (t.topup_time >= $4::timestamp AND t.topup_time <= $5::timestamp))
                GROUP BY EXTRACT(YEAR FROM t.topup_time), EXTRACT(MONTH FROM t.topup_time)
            ), formatted_data AS (
                SELECT year::text, TO_CHAR(TO_DATE(month::text, 'MM'), 'Mon') AS month, %s, total_amount FROM monthly_data
                UNION ALL
                SELECT EXTRACT(YEAR FROM $2::timestamp)::text, TO_CHAR($2::timestamp, 'Mon'), 0, 0 WHERE NOT EXISTS (SELECT 1 FROM monthly_data WHERE year = EXTRACT(YEAR FROM $2::timestamp)::integer AND month = EXTRACT(MONTH FROM $2::timestamp)::integer)
                UNION ALL
                SELECT EXTRACT(YEAR FROM $4::timestamp)::text, TO_CHAR($4::timestamp, 'Mon'), 0, 0 WHERE NOT EXISTS (SELECT 1 FROM monthly_data WHERE year = EXTRACT(YEAR FROM $4::timestamp)::integer AND month = EXTRACT(MONTH FROM $4::timestamp)::integer)
            )
            SELECT * FROM formatted_data ORDER BY year DESC, TO_DATE(month, 'Mon') DESC
            """,
        countCol, countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(req.getStatus(), curr, endCurr, prev, endPrev))
        .map(rows -> {
          List<TopupStats.MonthStatus> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TopupStats.MonthStatus.fromRow(r, countCol));
          return list;
        });
  }

  @Override
  public Future<List<TopupStats.YearStatus>> getYearlyTopupStatus(YearTopupStatusRequest req) {
    int endYear = req.getYear();
    String countCol = "total_" + req.getStatus();
    String sql = String.format(
        """
            WITH yearly_data AS (
                SELECT EXTRACT(YEAR FROM t.topup_time)::integer AS year, COUNT(*) AS %s, COALESCE(SUM(t.topup_amount), 0)::integer AS total_amount
                FROM topups t WHERE t.deleted_at IS NULL AND t.status = $1 AND (EXTRACT(YEAR FROM t.topup_time) = $2::integer OR EXTRACT(YEAR FROM t.topup_time) = $2::integer - 1)
                GROUP BY EXTRACT(YEAR FROM t.topup_time)
            ), formatted_data AS (
                SELECT year::text, %s::integer, total_amount FROM yearly_data
                UNION ALL
                SELECT $2::integer::text, 0::integer, 0::integer WHERE NOT EXISTS (SELECT 1 FROM yearly_data WHERE year = $2::integer)
                UNION ALL
                SELECT ($2::integer - 1)::text, 0::integer, 0::integer WHERE NOT EXISTS (SELECT 1 FROM yearly_data WHERE year = $2::integer - 1)
            )
            SELECT * FROM formatted_data ORDER BY year DESC
            """,
        countCol, countCol);

    return pool.preparedQuery(sql).execute(Tuple.of(req.getStatus(), endYear))
        .map(rows -> {
          List<TopupStats.YearStatus> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TopupStats.YearStatus.fromRow(r, countCol));
          return list;
        });
  }
}
