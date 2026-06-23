package io.example.transaction.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.transaction.domain.requests.YearCardNumberTransactionRequest;
import io.example.transaction.domain.requests.YearTransactionRequest;
import io.example.transaction.model.TransactionStats;
import io.example.transaction.repository.TransactionStatsMethodRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransactionStatsMethodRepositoryImpl implements TransactionStatsMethodRepository {
    private final Pool pool;

    private OffsetDateTime getYearStart(int year) {
        return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    }

    @Override
    public Future<List<TransactionStats.MonthMethod>> getMonthlyMethods(YearTransactionRequest request) {
        String sql = """
                WITH months AS (
                    SELECT generate_series(
                        date_trunc('year', $1::timestamp),
                        date_trunc('year', $1::timestamp) + interval '1 year' - interval '1 day',
                        interval '1 month'
                    ) AS month
                ), payment_methods AS (
                    SELECT DISTINCT payment_method FROM transactions WHERE deleted_at IS NULL
                )
                SELECT
                    TO_CHAR(m.month, 'Mon') AS month,
                    pm.payment_method,
                    COALESCE(COUNT(t.transaction_id), 0)::integer AS total_transactions,
                    COALESCE(SUM(t.amount), 0)::integer AS total_amount
                FROM months m
                CROSS JOIN payment_methods pm
                LEFT JOIN transactions t ON EXTRACT(MONTH FROM t.transaction_time) = EXTRACT(MONTH FROM m.month)
                    AND EXTRACT(YEAR FROM t.transaction_time) = EXTRACT(YEAR FROM m.month)
                    AND t.payment_method = pm.payment_method
                    AND t.deleted_at IS NULL
                GROUP BY m.month, pm.payment_method
                ORDER BY m.month, pm.payment_method;
                """;
        return pool.preparedQuery(sql).execute(Tuple.of(getYearStart((int) request.getYear())))
                .map(rows -> {
                    List<TransactionStats.MonthMethod> l = new ArrayList<>();
                    for (Row r : rows)
                        l.add(TransactionStats.MonthMethod.fromRow(r));
                    return l;
                });
    }

    @Override
    public Future<List<TransactionStats.YearMethod>> getYearlyMethods(YearTransactionRequest request) {
        int endYear = (int) request.getYear();
        String sql = """
                SELECT
                    EXTRACT(YEAR FROM t.created_at)::integer AS year,
                    t.payment_method,
                    COUNT(t.transaction_id)::integer AS total_transactions,
                    COALESCE(SUM(t.amount), 0)::integer AS total_amount
                FROM transactions t
                WHERE t.deleted_at IS NULL
                    AND EXTRACT(YEAR FROM t.created_at) >= $1::integer - 4
                    AND EXTRACT(YEAR FROM t.created_at) <= $1::integer
                GROUP BY EXTRACT(YEAR FROM t.created_at), t.payment_method
                ORDER BY year;
                """;
        return pool.preparedQuery(sql).execute(Tuple.of(endYear))
                .map(rows -> {
                    List<TransactionStats.YearMethod> l = new ArrayList<>();
                    for (Row r : rows)
                        l.add(TransactionStats.YearMethod.fromRow(r));
                    return l;
                });
    }

    @Override
    public Future<List<TransactionStats.MonthMethod>> getMonthlyMethodsByCard(
            YearCardNumberTransactionRequest request) {
        String sql = """
                WITH months AS (
                    SELECT generate_series(
                        date_trunc('year', $2::timestamp),
                        date_trunc('year', $2::timestamp) + interval '1 year' - interval '1 day',
                        interval '1 month'
                    ) AS month
                ), payment_methods AS (
                    SELECT DISTINCT payment_method FROM transactions WHERE deleted_at IS NULL
                )
                SELECT
                    TO_CHAR(m.month, 'Mon') AS month,
                    pm.payment_method,
                    COALESCE(COUNT(t.transaction_id), 0)::integer AS total_transactions,
                    COALESCE(SUM(t.amount), 0)::integer AS total_amount
                FROM months m
                CROSS JOIN payment_methods pm
                LEFT JOIN transactions t ON EXTRACT(MONTH FROM t.transaction_time) = EXTRACT(MONTH FROM m.month)
                    AND EXTRACT(YEAR FROM t.transaction_time) = EXTRACT(YEAR FROM m.month)
                    AND t.payment_method = pm.payment_method
                    AND t.card_number = $1
                    AND t.deleted_at IS NULL
                GROUP BY m.month, pm.payment_method
                ORDER BY m.month, pm.payment_method;
                """;
        return pool.preparedQuery(sql).execute(Tuple.of(request.getCardNumber(), getYearStart((int) request.getYear())))
                .map(rows -> {
                    List<TransactionStats.MonthMethod> l = new ArrayList<>();
                    for (Row r : rows)
                        l.add(TransactionStats.MonthMethod.fromRow(r));
                    return l;
                });
    }

    @Override
    public Future<List<TransactionStats.YearMethod>> getYearlyMethodsByCard(
            YearCardNumberTransactionRequest request) {
        int endYear = (int) request.getYear();
        String sql = """
                SELECT
                    EXTRACT(YEAR FROM t.created_at)::integer AS year,
                    t.payment_method,
                    COUNT(t.transaction_id)::integer AS total_transactions,
                    COALESCE(SUM(t.amount), 0)::integer AS total_amount
                FROM transactions t
                WHERE t.deleted_at IS NULL
                    AND t.card_number = $1
                    AND EXTRACT(YEAR FROM t.created_at) >= $2::integer - 4
                    AND EXTRACT(YEAR FROM t.created_at) <= $2::integer
                GROUP BY EXTRACT(YEAR FROM t.created_at), t.payment_method
                ORDER BY year;
                """;
        return pool.preparedQuery(sql).execute(Tuple.of(request.getCardNumber(), endYear))
                .map(rows -> {
                    List<TransactionStats.YearMethod> l = new ArrayList<>();
                    for (Row r : rows)
                        l.add(TransactionStats.YearMethod.fromRow(r));
                    return l;
                });
    }
}
