package io.example.saldo.repository.impl;

import io.example.saldo.domain.requests.MonthTotalSaldoBalance;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class SaldoStatsTotalRepositoryImplTest {

  @Mock
  private Pool pool;

  @Mock
  private PreparedQuery<RowSet<Row>> preparedQuery;

  @Mock
  private RowSet<Row> rowSet;

  @Mock
  private RowIterator<Row> iterator;

  @Mock
  private Row row;

  private SaldoStatsTotalRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new SaldoStatsTotalRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  /* ─── getMonthlyTotalSaldoBalance ─── */

  @Test
  @DisplayName("getMonthlyTotalSaldoBalance returns list of monthly totals")
  void getMonthlyTotalSaldoBalance(VertxTestContext ctx) {
    mockPool();

    when(row.getString("month")).thenReturn("06");
    when(row.getString("year")).thenReturn("2026");
    when(row.getLong("total_balance")).thenReturn(500_000L);

    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getMonthlyTotalSaldoBalance(MonthTotalSaldoBalance.builder().year(2026).month(6).build())
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo("06");
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalBalance()).isEqualTo(500_000L);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyTotalSaldoBalances ─── */

  @Test
  @DisplayName("getYearlyTotalSaldoBalances returns list of yearly totals")
  void getYearlyTotalSaldoBalances(VertxTestContext ctx) {
    mockPool();

    // YearTotalBalance.fromRow uses row.getValue("year"), not row.getString("year")
    when(row.getValue("year")).thenReturn("2026");
    when(row.getLong("total_balance")).thenReturn(3_000_000L);

    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getYearlyTotalSaldoBalances(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalBalance()).isEqualTo(3_000_000L);
          ctx.completeNow();
        })));
  }
}
