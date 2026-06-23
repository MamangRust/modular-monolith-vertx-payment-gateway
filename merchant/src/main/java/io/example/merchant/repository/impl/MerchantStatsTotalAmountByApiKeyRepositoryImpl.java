package io.example.merchant.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.merchant.domain.requests.merchant.MonthYearTotalAmountApiKey;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsTotalAmountByApiKeyRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MerchantStatsTotalAmountByApiKeyRepositoryImpl implements MerchantStatsTotalAmountByApiKeyRepository {
  private final Pool pool;

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  @Override
  public Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmountByApikey(
      MonthYearTotalAmountApiKey req) {
    String sql = """
        WITH monthly_data AS (
            SELECT EXTRACT(YEAR FROM t.transaction_time)::integer AS year,
                   EXTRACT(MONTH FROM t.transaction_time)::integer AS month,
                   COALESCE(SUM(t.amount), 0)::bigint AS amount
            FROM transactions t
            INNER JOIN merchants m ON t.merchant_id = m.merchant_id
            WHERE t.deleted_at IS NULL AND m.deleted_at IS NULL
              AND m.api_key = $2
              AND EXTRACT(YEAR FROM t.transaction_time) = EXTRACT(YEAR FROM $1::timestamp)
            GROUP BY EXTRACT(YEAR FROM t.transaction_time), EXTRACT(MONTH FROM t.transaction_time)
        ), formatted_data AS (
            SELECT year::text, TO_CHAR(TO_DATE(month::text, 'MM'), 'Mon') AS month, amount FROM monthly_data
            UNION ALL
            SELECT EXTRACT(YEAR FROM gs.month)::text AS year, TO_CHAR(gs.month, 'Mon') AS month, 0::bigint AS amount
            FROM generate_series(date_trunc('year', $1::timestamp), date_trunc('year', $1::timestamp) + interval '11 month', interval '1 month') AS gs(month)
            WHERE NOT EXISTS (
                SELECT 1 FROM monthly_data md
                WHERE md.year = EXTRACT(YEAR FROM gs.month)::integer
                  AND md.month = EXTRACT(MONTH FROM gs.month)::integer
            )
        )
        SELECT month, amount FROM formatted_data
        ORDER BY year ASC, TO_DATE(month, 'Mon') ASC
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(getYearStart(req.getYear()), req.getApikey()))
        .map(rows -> {
          List<MerchantStats.MonthAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(MerchantStats.MonthAmount.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<MerchantStats.YearAmount>> getYearlyTotalAmountByApikey(
      MonthYearTotalAmountApiKey req) {
    String sql = """
        WITH yearly_data AS (
            SELECT EXTRACT(YEAR FROM t.transaction_time)::integer AS year, COALESCE(SUM(t.amount), 0)::bigint AS amount
            FROM transactions t
            INNER JOIN merchants m ON t.merchant_id = m.merchant_id
            WHERE t.deleted_at IS NULL AND m.deleted_at IS NULL
              AND m.api_key = $2
              AND EXTRACT(YEAR FROM t.transaction_time) >= $1::integer - 4
              AND EXTRACT(YEAR FROM t.transaction_time) <= $1::integer
            GROUP BY EXTRACT(YEAR FROM t.transaction_time)
        ), formatted_data AS (
            SELECT year::text, amount FROM yearly_data
            UNION ALL
            SELECT y::text AS year, 0::bigint AS amount
            FROM generate_series($1::integer - 4, $1::integer) AS y
            WHERE NOT EXISTS (
                SELECT 1 FROM yearly_data yd
                WHERE yd.year = y
            )
        )
        SELECT year, amount FROM formatted_data
        ORDER BY year DESC
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(req.getYear(), req.getApikey()))
        .map(rows -> {
          List<MerchantStats.YearAmount> list = new ArrayList<>();
          for (Row r : rows)
            list.add(MerchantStats.YearAmount.fromRow(r));
          return list;
        });
  }
}
