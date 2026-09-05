package io.example.transfer.repository.impl;

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
class TransferStatsAmountRepositoryImplTest {

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

  private TransferStatsAmountRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TransferStatsAmountRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockMonthAmountRow() {
    when(row.getString("month")).thenReturn("Jan");
    when(row.getValue("total_transfer_amount")).thenReturn(50000L);
  }

  private void mockYearAmountRow() {
    when(row.getValue("year")).thenReturn(2026);
    when(row.getValue("total_transfer_amount")).thenReturn(60000L);
  }

  private void stubSingleRow() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
  }

  private void stubNoRows() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);
  }

  /* ─── getMonthlyTransferAmounts ─── */

  @Test
  @DisplayName("getMonthlyTransferAmounts returns monthly amounts list")
  void getMonthlyTransferAmountsSuccess(VertxTestContext ctx) {
    mockPool();
    mockMonthAmountRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getMonthlyTransferAmounts(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo("Jan");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(50000L);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyTransferAmounts ─── */

  @Test
  @DisplayName("getYearlyTransferAmounts returns yearly amounts list")
  void getYearlyTransferAmountsSuccess(VertxTestContext ctx) {
    mockPool();
    mockYearAmountRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getYearlyTransferAmounts(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(60000L);
          ctx.completeNow();
        })));
  }
}
