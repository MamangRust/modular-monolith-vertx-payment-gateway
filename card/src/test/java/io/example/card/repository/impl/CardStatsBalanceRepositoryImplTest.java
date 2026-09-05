package io.example.card.repository.impl;

import io.example.card.model.CardStats;
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
class CardStatsBalanceRepositoryImplTest {

  @Mock
  private Pool pool;

  @Mock
  private PreparedQuery<RowSet<Row>> preparedQuery;

  @Mock
  private RowSet<Row> rowSet;

  @Mock
  private RowIterator<Row> iterator;

  @Mock
  private Row row1;

  @Mock
  private Row row2;

  private CardStatsBalanceRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new CardStatsBalanceRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  /* ─── getMonthlyBalances ─── */

  @Test
  @DisplayName("getMonthlyBalances returns list of monthly balances")
  void getMonthlyBalances(VertxTestContext ctx) {
    mockPool();

    when(row1.getInteger("month")).thenReturn(1);
    when(row1.getLong("balance")).thenReturn(1000L);
    when(row2.getInteger("month")).thenReturn(2);
    when(row2.getLong("balance")).thenReturn(2000L);

    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, true, false);
    when(iterator.next()).thenReturn(row1, row2);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getMonthlyBalances(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(2);
          assertThat(list.get(0).getMonth()).isEqualTo(1);
          assertThat(list.get(0).getBalance()).isEqualTo(1000L);
          assertThat(list.get(1).getMonth()).isEqualTo(2);
          assertThat(list.get(1).getBalance()).isEqualTo(2000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyBalances returns empty list when no data")
  void getMonthlyBalancesEmpty(VertxTestContext ctx) {
    mockPool();

    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getMonthlyBalances(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).isEmpty();
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyBalances ─── */

  @Test
  @DisplayName("getYearlyBalances returns list of yearly balances")
  void getYearlyBalances(VertxTestContext ctx) {
    mockPool();

    when(row1.getInteger("year")).thenReturn(2022);
    when(row1.getLong("balance")).thenReturn(12000L);
    when(row2.getInteger("year")).thenReturn(2023);
    when(row2.getLong("balance")).thenReturn(15000L);

    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, true, false);
    when(iterator.next()).thenReturn(row1, row2);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getYearlyBalances(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(2);
          assertThat(list.get(0).getYear()).isEqualTo(2022);
          assertThat(list.get(1).getYear()).isEqualTo(2023);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyBalances returns empty list when no data")
  void getYearlyBalancesEmpty(VertxTestContext ctx) {
    mockPool();

    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getYearlyBalances(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).isEmpty();
          ctx.completeNow();
        })));
  }
}
