package io.example.merchant.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.repository.MerchantStatsMethodByMerchantRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class MerchantStatsMethodByMerchantRepositoryImpl implements MerchantStatsMethodByMerchantRepository {
  private final Pool pool;

  public MerchantStatsMethodByMerchantRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  @Override
  public Future<List<MerchantStats.MonthMethod>> getMonthlyPaymentMethodByMerchants(pb.merchant.Merchant.FindYearMerchantById req) {
    String sql = """
        WITH months AS (
            SELECT generate_series(
                date_trunc('year', $1::timestamp), date_trunc('year', $1::timestamp) + interval '1 year' - interval '1 day', interval '1 month'
            ) AS month
        ),
        payment_methods AS (
            SELECT DISTINCT payment_method FROM transactions WHERE deleted_at IS NULL
        )
        SELECT TO_CHAR(m.month, 'Mon') AS month, pm.payment_method, COALESCE(SUM(t.amount), 0)::bigint AS amount
        FROM months m
        CROSS JOIN payment_methods pm
        LEFT JOIN transactions t ON EXTRACT(MONTH FROM t.transaction_time) = EXTRACT(MONTH FROM m.month)
            AND EXTRACT(YEAR FROM t.transaction_time) = EXTRACT(YEAR FROM m.month)
            AND t.payment_method = pm.payment_method
            AND t.deleted_at IS NULL
            AND t.merchant_id = $2
        GROUP BY m.month, pm.payment_method
        ORDER BY m.month, pm.payment_method
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(getYearStart(req.getYear()), req.getMerchantId()))
        .map(rows -> {
          List<MerchantStats.MonthMethod> list = new ArrayList<>();
          for (Row r : rows) list.add(MerchantStats.MonthMethod.fromRow(r));
          return list;
        });
  }

  @Override
  public Future<List<MerchantStats.YearMethod>> getYearlyPaymentMethodByMerchants(pb.merchant.Merchant.FindYearMerchantById req) {
    String sql = """
        WITH last_five_years AS (
            SELECT EXTRACT(YEAR FROM t.transaction_time) AS year, t.payment_method, SUM(t.amount) AS amount
            FROM transactions t
            JOIN merchants m ON t.merchant_id = m.merchant_id
            WHERE t.deleted_at IS NULL AND m.deleted_at IS NULL
              AND t.merchant_id = $2
              AND EXTRACT(YEAR FROM t.transaction_time) >= $1::int - 4
              AND EXTRACT(YEAR FROM t.transaction_time) <= $1::int
            GROUP BY EXTRACT(YEAR FROM t.transaction_time), t.payment_method
        )
        SELECT year::text, payment_method, COALESCE(amount, 0)::bigint AS amount 
        FROM last_five_years 
        ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(req.getYear(), req.getMerchantId()))
        .map(rows -> {
          List<MerchantStats.YearMethod> list = new ArrayList<>();
          for (Row r : rows) list.add(MerchantStats.YearMethod.fromRow(r));
          return list;
        });
  }
}
