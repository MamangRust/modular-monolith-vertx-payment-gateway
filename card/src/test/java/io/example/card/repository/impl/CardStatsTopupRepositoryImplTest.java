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
class CardStatsTopupRepositoryImplTest {

  @Mock private Pool pool;
  @Mock private PreparedQuery<RowSet<Row>> preparedQuery;
  @Mock private RowSet<Row> rowSet;
  @Mock private RowIterator<Row> iterator;
  @Mock private Row row;
  private CardStatsTopupRepositoryImpl repo;

  @BeforeEach void setUp() { repo = new CardStatsTopupRepositoryImpl(pool); }

  private void mockPool() { when(pool.preparedQuery(anyString())).thenReturn(preparedQuery); }

  @Test @DisplayName("getMonthlyTopupAmount returns list")
  void getMonthlyTopupAmount(VertxTestContext ctx) {
    mockPool();
    when(row.getInteger("month")).thenReturn(1);
    when(row.getLong("amount")).thenReturn(5000L);
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));
    repo.getMonthlyTopupAmount(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test @DisplayName("getYearlyTopupAmount returns list")
  void getYearlyTopupAmount(VertxTestContext ctx) {
    mockPool();
    when(row.getInteger("year")).thenReturn(2025);
    when(row.getLong("amount")).thenReturn(60000L);
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));
    repo.getYearlyTopupAmount(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo(2025);
          ctx.completeNow();
        })));
  }
}
