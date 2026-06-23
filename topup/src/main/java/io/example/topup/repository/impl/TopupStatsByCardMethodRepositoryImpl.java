package io.example.topup.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.topup.domain.requests.topup.YearTopupCardNumberRequest;
import io.example.topup.model.TopupStats;
import io.example.topup.repository.TopupStatsByCardMethodRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TopupStatsByCardMethodRepositoryImpl implements TopupStatsByCardMethodRepository {
  private final Pool pool;

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  @Override
  public Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethodsByCard(YearTopupCardNumberRequest req) {
    int year = req.getYear();
    String card = req.getCardNumber();
    String sql = """
        WITH months AS (
            SELECT generate_series(date_trunc('year', $2::timestamp), date_trunc('year', $2::timestamp) + interval '1 year' - interval '1 day', interval '1 month') AS month
        ), topup_methods AS (
            SELECT DISTINCT topup_method FROM topups WHERE deleted_at IS NULL
        )
        SELECT TO_CHAR(m.month, 'Mon') AS month, tm.topup_method, COALESCE(COUNT(t.topup_id), 0)::integer AS total_topups, COALESCE(SUM(t.topup_amount), 0)::integer AS total_amount
        FROM months m CROSS JOIN topup_methods tm
        LEFT JOIN topups t ON EXTRACT(MONTH FROM t.topup_time) = EXTRACT(MONTH FROM m.month)
            AND EXTRACT(YEAR FROM t.topup_time) = EXTRACT(YEAR FROM m.month) AND t.topup_method = tm.topup_method AND t.card_number = $1 AND t.deleted_at IS NULL
        GROUP BY m.month, tm.topup_method ORDER BY m.month, tm.topup_method
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(card, getYearStart(year)))
        .map(rows -> {
          List<TopupStats.MonthMethod> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TopupStats.MonthMethod.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<TopupStats.YearMethod>> getYearlyTopupMethodsByCard(YearTopupCardNumberRequest req) {
    int endYear = req.getYear();
    String card = req.getCardNumber();
    String sql = """
        SELECT EXTRACT(YEAR FROM t.topup_time)::integer AS year, t.topup_method, COUNT(t.topup_id)::integer AS total_topups, COALESCE(SUM(t.topup_amount), 0)::integer AS total_amount
        FROM topups t WHERE t.deleted_at IS NULL AND t.card_number = $1 AND EXTRACT(YEAR FROM t.topup_time) >= $2::integer - 4 AND EXTRACT(YEAR FROM t.topup_time) <= $2::integer
        GROUP BY EXTRACT(YEAR FROM t.topup_time), t.topup_method ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(card, endYear))
        .map(rows -> {
          List<TopupStats.YearMethod> list = new ArrayList<>();
          for (Row r : rows)
            list.add(TopupStats.YearMethod.fromRow(r));
          return list;
        });
  }
}
