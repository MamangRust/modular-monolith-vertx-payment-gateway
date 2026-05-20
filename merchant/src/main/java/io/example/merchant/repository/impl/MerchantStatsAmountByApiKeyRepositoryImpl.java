package io.example.merchant.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsAmountByApiKeyRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class MerchantStatsAmountByApiKeyRepositoryImpl implements MerchantStatsAmountByApiKeyRepository {
  private final Pool pool;

  public MerchantStatsAmountByApiKeyRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  @Override
  public Future<List<MerchantStats.MonthAmount>> getMonthlyAmountByApikey(pb.merchant.Merchant.FindYearMerchantByApikey req) {
    String sql = """
        WITH months AS (
            SELECT generate_series(
                date_trunc('year', $1::timestamp), date_trunc('year', $1::timestamp) + interval '1 year' - interval '1 day', interval '1 month'
            ) AS month
        )
        SELECT TO_CHAR(m.month, 'Mon') AS month, COALESCE(SUM(t.amount), 0)::bigint AS amount
        FROM months m
        LEFT JOIN transactions t ON EXTRACT(MONTH FROM t.transaction_time) = EXTRACT(MONTH FROM m.month)
            AND EXTRACT(YEAR FROM t.transaction_time) = EXTRACT(YEAR FROM m.month)
            AND t.deleted_at IS NULL
            AND EXISTS (
                SELECT 1 FROM merchants mch 
                WHERE mch.merchant_id = t.merchant_id 
                AND mch.deleted_at IS NULL 
                AND mch.api_key = $2
            )
        GROUP BY m.month
        ORDER BY m.month
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(getYearStart(req.getYear()), req.getApiKey()))
        .map(rows -> {
          List<MerchantStats.MonthAmount> list = new ArrayList<>();
          for (Row r : rows) list.add(MerchantStats.MonthAmount.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<MerchantStats.YearAmount>> getYearlyAmountByApikey(pb.merchant.Merchant.FindYearMerchantByApikey req) {
    String sql = """
        WITH last_five_years AS (
            SELECT EXTRACT(YEAR FROM t.transaction_time) AS year, SUM(t.amount) AS amount
            FROM transactions t
            JOIN merchants m ON t.merchant_id = m.merchant_id
            WHERE t.deleted_at IS NULL AND m.deleted_at IS NULL
              AND m.api_key = $2
              AND EXTRACT(YEAR FROM t.transaction_time) >= $1::int - 4
              AND EXTRACT(YEAR FROM t.transaction_time) <= $1::int
            GROUP BY EXTRACT(YEAR FROM t.transaction_time)
        )
        SELECT year::text, COALESCE(amount, 0)::bigint AS amount 
        FROM last_five_years 
        ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(req.getYear(), req.getApiKey()))
        .map(rows -> {
          List<MerchantStats.YearAmount> list = new ArrayList<>();
          for (Row r : rows) list.add(MerchantStats.YearAmount.fromRow(r));
          return list;
        });
  }
}
