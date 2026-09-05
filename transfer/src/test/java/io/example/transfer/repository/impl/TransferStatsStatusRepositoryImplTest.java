package io.example.transfer.repository.impl;

import io.example.transfer.domain.requests.MonthStatusTransfer;
import io.example.transfer.domain.requests.YearStatusTransferRequest;
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
class TransferStatsStatusRepositoryImplTest {

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

  private TransferStatsStatusRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TransferStatsStatusRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void stubPagedRows() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
  }

  /* ─── getMonthlyTransferStatus ─── */

  @Test
  @DisplayName("getMonthlyTransferStatus returns monthly status counts")
  void getMonthlyTransferStatus(VertxTestContext ctx) {
    mockPool();

    when(row.getString("year")).thenReturn("2026");
    when(row.getString("month")).thenReturn("Jun");
    when(row.getValue("total_completed")).thenReturn(10);
    when(row.getValue("total_amount")).thenReturn(100_000L);
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = MonthStatusTransfer.builder()
        .year(2026).month(6).status("completed").build();

    repo.getMonthlyTransferStatus(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getMonth()).isEqualTo("Jun");
          assertThat(list.get(0).getTotalCount()).isEqualTo(10L);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(100_000L);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyTransferStatus ─── */

  @Test
  @DisplayName("getYearlyTransferStatus returns yearly status counts")
  void getYearlyTransferStatus(VertxTestContext ctx) {
    mockPool();

    when(row.getString("year")).thenReturn("2026");
    when(row.getValue("total_completed")).thenReturn(50);
    when(row.getValue("total_amount")).thenReturn(500_000L);
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearStatusTransferRequest.builder()
        .year(2026).status("completed").build();

    repo.getYearlyTransferStatus(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalCount()).isEqualTo(50L);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(500_000L);
          ctx.completeNow();
        })));
  }
}
