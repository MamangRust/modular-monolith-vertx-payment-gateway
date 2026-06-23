package io.example.card.repository.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsBalanceByCardRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CardStatsBalanceByCardRepositoryImpl implements CardStatsBalanceByCardRepository {
  private final Pool pool;

  private OffsetDateTime getYearStart(int year) {
    return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  }

  @Override
  public Future<List<CardStats.MonthBalance>> getMonthlyBalancesByCardNumber(MonthYearCardNumberCard req) {
    return pool
        .preparedQuery(
            """
                WITH months AS (SELECT generate_series(date_trunc('year', $1::timestamp), date_trunc('year', $1::timestamp) + interval '1 year' - interval '1 day', interval '1 month') AS m)
                SELECT EXTRACT(MONTH FROM m.m)::int AS month, COALESCE(SUM(s.total_balance), 0)::bigint AS balance
                FROM months m
                LEFT JOIN saldos s ON EXTRACT(MONTH FROM s.created_at) = EXTRACT(MONTH FROM m.m) AND EXTRACT(YEAR FROM s.created_at) = EXTRACT(YEAR FROM m.m) AND s.deleted_at IS NULL AND s.card_number = $2
                GROUP BY m.m ORDER BY m.m
                """)
        .execute(Tuple.of(getYearStart(req.getYear()), req.getCardNumber())).map(this::mapMonthBalanceList);
  }

  @Override
  public Future<List<CardStats.YearlyBalance>> getYearlyBalancesByCardNumber(MonthYearCardNumberCard req) {
    return pool
        .preparedQuery(
            """
                SELECT EXTRACT(YEAR FROM s.created_at)::int AS year, SUM(s.total_balance)::bigint AS balance
                FROM saldos s WHERE s.deleted_at IS NULL AND s.card_number = $2 AND EXTRACT(YEAR FROM s.created_at) BETWEEN $1 - 4 AND $1
                GROUP BY 1 ORDER BY 1
                """)
        .execute(Tuple.of(req.getYear(), req.getCardNumber())).map(this::mapYearBalanceList);
  }

  private List<CardStats.MonthBalance> mapMonthBalanceList(RowSet<Row> rows) {
    List<CardStats.MonthBalance> list = new ArrayList<>();
    for (Row row : rows)
      list.add(CardStats.MonthBalance.fromRow(row));
    return list;
  }

  private List<CardStats.YearlyBalance> mapYearBalanceList(RowSet<Row> rows) {
    List<CardStats.YearlyBalance> list = new ArrayList<>();
    for (Row row : rows)
      list.add(CardStats.YearlyBalance.fromRow(row));
    return list;
  }
}
