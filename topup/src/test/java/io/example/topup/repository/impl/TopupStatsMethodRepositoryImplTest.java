package io.example.topup.repository.impl;

import io.example.topup.domain.requests.topup.YearTopupRequest;
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
class TopupStatsMethodRepositoryImplTest {

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

  private TopupStatsMethodRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TopupStatsMethodRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void stubSingleRow() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
  }

  /* ─── monthly ─── */

  @Test
  @DisplayName("getMonthlyTopupMethods returns list of monthly method stats")
  void monthly(VertxTestContext ctx) {
    mockPool();
    when(row.getString("month")).thenReturn("Jan");
    when(row.getString("topup_method")).thenReturn("BANK");
    when(row.getValue("total_topups")).thenReturn(5L);
    when(row.getValue("total_amount")).thenReturn(50000L);
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getMonthlyTopupMethods(YearTopupRequest.builder().year(2026).build())
        .onComplete(ctx.succeeding(l -> ctx.verify(() -> {
          assertThat(l).hasSize(1);
          ctx.completeNow();
        })));
  }

  /* ─── yearly ─── */

  @Test
  @DisplayName("getYearlyTopupMethods returns list of yearly method stats")
  void yearly(VertxTestContext ctx) {
    mockPool();
    when(row.getValue("year")).thenReturn("2025");
    when(row.getString("topup_method")).thenReturn("BANK");
    when(row.getValue("total_topups")).thenReturn(60L);
    when(row.getValue("total_amount")).thenReturn(600000L);
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getYearlyTopupMethods(YearTopupRequest.builder().year(2026).build())
        .onComplete(ctx.succeeding(l -> ctx.verify(() -> {
          assertThat(l).hasSize(1);
          ctx.completeNow();
        })));
  }
}
