package io.example.merchant.repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.model.MerchantTransactions;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class MerchantStatsRepository {
  private final Pool pool;

  public MerchantStatsRepository(Pool pool) {
    this.pool = pool;
  }

  private String normalizeSearch(String search) {
    return (search == null || search.isBlank()) ? null : search;
  }

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  // --- TRANSACTION FEEDS ---

  public Future<PagedResult<MerchantTransactions>> getTransactions(int page, int pageSize, String search) {
    int offset = Math.max(0, page - 1) * pageSize;
    String sql = """
        SELECT t.transaction_id, t.card_number, t.amount, t.payment_method, t.merchant_id, m.name AS merchant_name,
               t.transaction_time, t.created_at, t.updated_at, t.deleted_at, COUNT(*) OVER() AS total_count
        FROM transactions t
        JOIN merchants m ON t.merchant_id = m.merchant_id
        WHERE t.deleted_at IS NULL
          AND ($1::TEXT IS NULL OR t.card_number ILIKE '%' || $1 || '%' OR t.payment_method ILIKE '%' || $1 || '%')
        ORDER BY t.transaction_time DESC LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(normalizeSearch(search), pageSize, offset))
        .map(rows -> {
          List<MerchantTransactions> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(MerchantTransactions.fromRow(row));
            if (total == 0) total = row.getInteger("total_count");
          }
          return new PagedResult<>(list, total);
        });
  }

  public Future<PagedResult<MerchantTransactions>> getTransactionsByMerchantId(int page, int pageSize, String search, int merchantId) {
    int offset = Math.max(0, page - 1) * pageSize;
    String sql = """
        SELECT t.transaction_id, t.card_number, t.amount, t.payment_method, t.merchant_id, m.name AS merchant_name,
               t.transaction_time, t.created_at, t.updated_at, t.deleted_at, COUNT(*) OVER() AS total_count
        FROM transactions t
        JOIN merchants m ON t.merchant_id = m.merchant_id
        WHERE t.deleted_at IS NULL AND t.merchant_id = $4
          AND ($1::TEXT IS NULL OR t.card_number ILIKE '%' || $1 || '%' OR t.payment_method ILIKE '%' || $1 || '%')
        ORDER BY t.transaction_time DESC LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(normalizeSearch(search), pageSize, offset, merchantId))
        .map(rows -> {
          List<MerchantTransactions> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(MerchantTransactions.fromRow(row));
            if (total == 0) total = row.getInteger("total_count");
          }
          return new PagedResult<>(list, total);
        });
  }

  public Future<PagedResult<MerchantTransactions>> getTransactionsByApiKey(int page, int pageSize, String search, String apiKey) {
    int offset = Math.max(0, page - 1) * pageSize;
    String sql = """
        SELECT t.transaction_id, t.card_number, t.amount, t.payment_method, t.merchant_id, m.name AS merchant_name,
               t.transaction_time, t.created_at, t.updated_at, t.deleted_at, COUNT(*) OVER() AS total_count
        FROM transactions t
        JOIN merchants m ON t.merchant_id = m.merchant_id
        WHERE t.deleted_at IS NULL AND m.api_key = $4
          AND ($1::TEXT IS NULL OR t.card_number ILIKE '%' || $1 || '%' OR t.payment_method ILIKE '%' || $1 || '%')
        ORDER BY t.transaction_time DESC LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(normalizeSearch(search), pageSize, offset, apiKey))
        .map(rows -> {
          List<MerchantTransactions> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(MerchantTransactions.fromRow(row));
            if (total == 0) total = row.getInteger("total_count");
          }
          return new PagedResult<>(list, total);
        });
  }

  // --- ANALYTICAL METRICS ---

  public Future<List<MerchantStats.MonthAmount>> getMonthlyAmounts(int year, Integer merchantId, String apiKey) {
    String sql = """
        WITH months AS (
            SELECT generate_series(
                date_trunc('year', $1::timestamp),
                date_trunc('year', $1::timestamp) + interval '1 year' - interval '1 day',
                interval '1 month'
            ) AS month
        )
        SELECT TO_CHAR(m.month, 'Mon') AS month, COALESCE(SUM(t.amount), 0)::bigint AS amount
        FROM months m
        LEFT JOIN transactions t ON EXTRACT(MONTH FROM t.transaction_time) = EXTRACT(MONTH FROM m.month)
            AND EXTRACT(YEAR FROM t.transaction_time) = EXTRACT(YEAR FROM m.month)
            AND t.deleted_at IS NULL
            AND ($2::int IS NULL OR t.merchant_id = $2)
        LEFT JOIN merchants mch ON t.merchant_id = mch.merchant_id AND mch.deleted_at IS NULL
        WHERE ($3::text IS NULL OR mch.api_key = $3)
        GROUP BY m.month ORDER BY m.month
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(getYearStart(year), merchantId, apiKey))
        .map(rows -> {
          List<MerchantStats.MonthAmount> list = new ArrayList<>();
          for (Row r : rows) list.add(MerchantStats.MonthAmount.fromRow(r));
          return list;
        });
  }

  public Future<List<MerchantStats.YearAmount>> getYearlyAmounts(int year, Integer merchantId, String apiKey) {
    String sql = """
        WITH last_five_years AS (
            SELECT EXTRACT(YEAR FROM t.transaction_time) AS year, SUM(t.amount) AS amount
            FROM transactions t
            JOIN merchants m ON t.merchant_id = m.merchant_id
            WHERE t.deleted_at IS NULL AND m.deleted_at IS NULL
              AND ($2::int IS NULL OR t.merchant_id = $2)
              AND ($3::text IS NULL OR m.api_key = $3)
              AND EXTRACT(YEAR FROM t.transaction_time) >= $1 - 4
              AND EXTRACT(YEAR FROM t.transaction_time) <= $1
            GROUP BY EXTRACT(YEAR FROM t.transaction_time)
        )
        SELECT year::text, COALESCE(amount, 0)::bigint AS amount FROM last_five_years ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(year, merchantId, apiKey))
        .map(rows -> {
          List<MerchantStats.YearAmount> list = new ArrayList<>();
          for (Row r : rows) list.add(MerchantStats.YearAmount.fromRow(r));
          return list;
        });
  }

  public Future<List<MerchantStats.MonthMethod>> getMonthlyMethodAmounts(int year, Integer merchantId, String apiKey) {
    String sql = """
        WITH months AS (
            SELECT generate_series(
                date_trunc('year', $1::timestamp),
                date_trunc('year', $1::timestamp) + interval '1 year' - interval '1 day',
                interval '1 month'
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
            AND ($2::int IS NULL OR t.merchant_id = $2)
        LEFT JOIN merchants mch ON t.merchant_id = mch.merchant_id AND mch.deleted_at IS NULL
        WHERE ($3::text IS NULL OR mch.api_key = $3)
        GROUP BY m.month, pm.payment_method ORDER BY m.month, pm.payment_method
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(getYearStart(year), merchantId, apiKey))
        .map(rows -> {
          List<MerchantStats.MonthMethod> list = new ArrayList<>();
          for (Row r : rows) list.add(MerchantStats.MonthMethod.fromRow(r));
          return list;
        });
  }

  public Future<List<MerchantStats.YearMethod>> getYearlyMethodAmounts(int year, Integer merchantId, String apiKey) {
    String sql = """
        WITH last_five_years AS (
            SELECT EXTRACT(YEAR FROM t.transaction_time) AS year, t.payment_method, SUM(t.amount) AS amount
            FROM transactions t
            JOIN merchants m ON t.merchant_id = m.merchant_id
            WHERE t.deleted_at IS NULL AND m.deleted_at IS NULL
              AND ($2::int IS NULL OR t.merchant_id = $2)
              AND ($3::text IS NULL OR m.api_key = $3)
              AND EXTRACT(YEAR FROM t.transaction_time) >= $1 - 4
              AND EXTRACT(YEAR FROM t.transaction_time) <= $1
            GROUP BY EXTRACT(YEAR FROM t.transaction_time), t.payment_method
        )
        SELECT year::text, payment_method, COALESCE(amount, 0)::bigint AS amount FROM last_five_years ORDER BY year
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(year, merchantId, apiKey))
        .map(rows -> {
          List<MerchantStats.YearMethod> list = new ArrayList<>();
          for (Row r : rows) list.add(MerchantStats.YearMethod.fromRow(r));
          return list;
        });
  }

  public Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmounts(int year, Integer merchantId, String apiKey) {
    String sql = """
        WITH monthly_data AS (
            SELECT EXTRACT(YEAR FROM t.transaction_time)::text AS year,
                   TO_CHAR(t.transaction_time, 'Mon') AS month,
                   COALESCE(SUM(t.amount), 0)::bigint AS amount
            FROM transactions t
            INNER JOIN merchants m ON t.merchant_id = m.merchant_id
            WHERE t.deleted_at IS NULL AND m.deleted_at IS NULL
              AND ($2::int IS NULL OR t.merchant_id = $2)
              AND ($3::text IS NULL OR m.api_key = $3)
              AND EXTRACT(YEAR FROM t.transaction_time) = EXTRACT(YEAR FROM $1::timestamp)
            GROUP BY EXTRACT(YEAR FROM t.transaction_time), TO_CHAR(t.transaction_time, 'Mon')
        ), formatted_data AS (
            SELECT year, month, amount FROM monthly_data
            UNION ALL
            SELECT EXTRACT(YEAR FROM gs.month)::text AS year, TO_CHAR(gs.month, 'Mon') AS month, 0::bigint AS amount
            FROM generate_series(date_trunc('year', $1::timestamp), date_trunc('year', $1::timestamp) + interval '11 month', interval '1 month') AS gs(month)
            WHERE NOT EXISTS (SELECT 1 FROM monthly_data md WHERE md.month = TO_CHAR(gs.month, 'Mon'))
        )
        SELECT month, amount FROM formatted_data ORDER BY TO_DATE(month, 'Mon') DESC
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(getYearStart(year), merchantId, apiKey))
        .map(rows -> {
          List<MerchantStats.MonthAmount> list = new ArrayList<>();
          for (Row r : rows) list.add(MerchantStats.MonthAmount.fromRow(r));
          return list;
        });
  }

  public Future<List<MerchantStats.YearAmount>> getYearlyTotalAmounts(int year, Integer merchantId, String apiKey) {
    String sql = """
        WITH yearly_data AS (
            SELECT EXTRACT(YEAR FROM t.transaction_time)::integer AS year, COALESCE(SUM(t.amount), 0)::bigint AS amount
            FROM transactions t
            INNER JOIN merchants m ON t.merchant_id = m.merchant_id
            WHERE t.deleted_at IS NULL AND m.deleted_at IS NULL
              AND ($2::int IS NULL OR t.merchant_id = $2)
              AND ($3::text IS NULL OR m.api_key = $3)
              AND EXTRACT(YEAR FROM t.transaction_time) >= $1::integer - 4
              AND EXTRACT(YEAR FROM t.transaction_time) <= $1::integer
            GROUP BY EXTRACT(YEAR FROM t.transaction_time)
        ), formatted_data AS (
            SELECT year::text, amount FROM yearly_data
            UNION ALL
            SELECT y::text AS year, 0::bigint AS amount
            FROM generate_series($1::integer - 4, $1::integer) AS y
            WHERE NOT EXISTS (SELECT 1 FROM yearly_data yd WHERE yd.year = y)
        )
        SELECT year, amount FROM formatted_data ORDER BY year DESC
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(year, merchantId, apiKey))
        .map(rows -> {
          List<MerchantStats.YearAmount> list = new ArrayList<>();
          for (Row r : rows) list.add(MerchantStats.YearAmount.fromRow(r));
          return list;
        });
  }
}
