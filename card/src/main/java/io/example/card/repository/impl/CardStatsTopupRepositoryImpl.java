package io.example.card.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsTopupRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class CardStatsTopupRepositoryImpl implements CardStatsTopupRepository {
  private final Pool pool;

  public CardStatsTopupRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  @Override
  public Future<List<CardStats.MonthAmount>> getMonthlyTopupAmount(int year) {
    return pool.preparedQuery("""
        WITH months AS (SELECT generate_series(date_trunc('year', $1::timestamp), date_trunc('year', $1::timestamp) + interval '1 year' - interval '1 day', interval '1 month') AS m)
        SELECT EXTRACT(MONTH FROM m.m)::int AS month, COALESCE(SUM(t.topup_amount), 0)::bigint AS amount
        FROM months m
        LEFT JOIN topups t ON EXTRACT(MONTH FROM t.topup_time) = EXTRACT(MONTH FROM m.m) AND EXTRACT(YEAR FROM t.topup_time) = EXTRACT(YEAR FROM m.m) AND t.deleted_at IS NULL
        GROUP BY m.m ORDER BY m.m
        """).execute(Tuple.of(getYearStart(year))).map(this::mapMonthAmountList);
  }

  @Override
  public Future<List<CardStats.YearAmount>> getYearlyTopupAmount(int endYear) {
    return pool.preparedQuery("""
        SELECT EXTRACT(YEAR FROM t.topup_time)::int AS year, SUM(t.topup_amount)::bigint AS amount
        FROM topups t WHERE t.deleted_at IS NULL AND EXTRACT(YEAR FROM t.topup_time) BETWEEN $1 - 4 AND $1
        GROUP BY 1 ORDER BY 1
        """).execute(Tuple.of(endYear)).map(this::mapYearAmountList);
  }

  private List<CardStats.MonthAmount> mapMonthAmountList(RowSet<Row> rows) {
    List<CardStats.MonthAmount> list = new ArrayList<>();
    for (Row row : rows) list.add(CardStats.MonthAmount.fromRow(row));
    return list;
  }

  private List<CardStats.YearAmount> mapYearAmountList(RowSet<Row> rows) {
    List<CardStats.YearAmount> list = new ArrayList<>();
    for (Row row : rows) list.add(CardStats.YearAmount.fromRow(row));
    return list;
  }
}
