package io.example.transaction.repository.impl;

import io.example.transaction.domain.requests.MonthStatusTransaction;
import io.example.transaction.domain.requests.MonthStatusTransactionCardNumber;
import io.example.transaction.domain.requests.YearStatusTransaction;
import io.example.transaction.domain.requests.YearStatusTransactionCardNumber;
import io.example.transaction.model.TransactionStats;
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
class TransactionStatsStatusRepositoryImplTest {

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

  private TransactionStatsStatusRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TransactionStatsStatusRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void stubSingleRow() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
  }

  @Test
  @DisplayName("getMonthlyStatus returns monthly transaction status counts")
  void monthlySuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getString("year")).thenReturn("2026");
    when(row.getString("month")).thenReturn("Jun");
    when(row.getValue("total_completed")).thenReturn(10);
    when(row.getValue("total_amount")).thenReturn(100000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = MonthStatusTransaction.builder()
        .year(2026)
        .month(6)
        .status("completed")
        .build();

    repo.getMonthlyStatus(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getMonth()).isEqualTo("Jun");
          assertThat(list.get(0).getTotalCount()).isEqualTo(10L);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(100000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyStatus returns yearly transaction status counts")
  void yearlySuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getString("year")).thenReturn("2026");
    when(row.getValue("total_completed")).thenReturn(50);
    when(row.getValue("total_amount")).thenReturn(500000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearStatusTransaction.builder()
        .year(2026)
        .status("completed")
        .build();

    repo.getYearlyStatus(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalCount()).isEqualTo(50L);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(500000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyStatusByCard returns monthly status counts filtered by card")
  void monthlyByCardSuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getString("year")).thenReturn("2026");
    when(row.getString("month")).thenReturn("Jun");
    when(row.getValue("total_completed")).thenReturn(3);
    when(row.getValue("total_amount")).thenReturn(30000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = MonthStatusTransactionCardNumber.builder()
        .cardNumber("4111111111111111")
        .year(2026)
        .month(6)
        .status("completed")
        .build();

    repo.getMonthlyStatusByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getMonth()).isEqualTo("Jun");
          assertThat(list.get(0).getTotalCount()).isEqualTo(3L);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(30000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyStatusByCard returns yearly status counts filtered by card")
  void yearlyByCardSuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getString("year")).thenReturn("2026");
    when(row.getValue("total_completed")).thenReturn(15);
    when(row.getValue("total_amount")).thenReturn(150000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearStatusTransactionCardNumber.builder()
        .cardNumber("4111111111111111")
        .year(2026)
        .status("completed")
        .build();

    repo.getYearlyStatusByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalCount()).isEqualTo(15L);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(150000L);
          ctx.completeNow();
        })));
  }
}
