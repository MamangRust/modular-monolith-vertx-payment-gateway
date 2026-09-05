package io.example.saldo.repository.impl;

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
class SaldoStatsBalanceRepositoryImplTest {

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

  private SaldoStatsBalanceRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new SaldoStatsBalanceRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  /* ─── getMonthlySaldoBalances ─── */

  @Test
  @DisplayName("getMonthlySaldoBalances returns list of monthly balances")
  void getMonthlySaldoBalances(VertxTestContext ctx) {
    mockPool();

    when(row.getString("month")).thenReturn("06");
    when(row.getLong("total_balance")).thenReturn(500_000L);

    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getMonthlySaldoBalances(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo("06");
          assertThat(list.get(0).getTotalBalance()).isEqualTo(500_000L);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlySaldoBalances ─── */

  @Test
  @DisplayName("getYearlySaldoBalances returns list of yearly balances")
  void getYearlySaldoBalances(VertxTestContext ctx) {
    mockPool();

    // YearBalance.fromRow uses row.getValue("year"), not row.getString("year")
    when(row.getValue("year")).thenReturn("2026");
    when(row.getLong("total_balance")).thenReturn(3_000_000L);

    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getYearlySaldoBalances(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalBalance()).isEqualTo(3_000_000L);
          ctx.completeNow();
        })));
  }
}
