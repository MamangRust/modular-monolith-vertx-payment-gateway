package io.example.topup.repository.impl;

import io.example.topup.domain.requests.topup.MonthTopupStatusRequest;
import io.example.topup.domain.requests.topup.YearTopupStatusRequest;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TopupStatsStatusRepositoryImplTest {

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

  private TopupStatsStatusRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TopupStatsStatusRepositoryImpl(pool);
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
  @DisplayName("getMonthlyTopupStatus returns list of monthly status stats")
  void monthly(VertxTestContext ctx) {
    mockPool();
    when(row.getString("year")).thenReturn("2026");
    when(row.getString("month")).thenReturn("Jan");
    when(row.getValue("total_count")).thenReturn(10L);
    when(row.getValue("total_amount")).thenReturn(50000L);
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getMonthlyTopupStatus(MonthTopupStatusRequest.builder().year(2026).month(1).status("SUCCESS").build())
        .onComplete(ctx.succeeding(l -> ctx.verify(() -> {
          assertThat(l).hasSize(1);
          ctx.completeNow();
        })));
  }

  /* ─── yearly ─── */

  @Test
  @DisplayName("getYearlyTopupStatus returns list of yearly status stats")
  void yearly(VertxTestContext ctx) {
    mockPool();
    when(row.getString("year")).thenReturn("2025");
    when(row.getValue("total_count")).thenReturn(120L);
    when(row.getValue("total_amount")).thenReturn(600000L);
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getYearlyTopupStatus(YearTopupStatusRequest.builder().year(2026).status("SUCCESS").build())
        .onComplete(ctx.succeeding(l -> ctx.verify(() -> {
          assertThat(l).hasSize(1);
          ctx.completeNow();
        })));
  }
}
