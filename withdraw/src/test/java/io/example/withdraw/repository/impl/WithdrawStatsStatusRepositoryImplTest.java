package io.example.withdraw.repository.impl;

import io.example.withdraw.domain.requests.MonthStatusWithdrawCardNumber;
import io.example.withdraw.domain.requests.YearStatusWithdrawCardNumber;
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
class WithdrawStatsStatusRepositoryImplTest {

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

  private WithdrawStatsStatusRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new WithdrawStatsStatusRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void stubPagedRows() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
  }

  /* ─── getMonthlyWithdrawStatus ─── */

  @Test
  @DisplayName("getMonthlyWithdrawStatus returns monthly status counts")
  void getMonthlyWithdrawStatus(VertxTestContext ctx) {
    mockPool();

    when(row.getString("year")).thenReturn("2026");
    when(row.getString("month")).thenReturn("Jun");
    when(row.getValue("total_completed")).thenReturn(10);
    when(row.getValue("total_amount")).thenReturn(100_000L);
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = MonthStatusWithdrawCardNumber.builder()
        .year(2026).month(6).status("completed").build();

    repo.getMonthlyWithdrawStatus(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getMonth()).isEqualTo("Jun");
          assertThat(list.get(0).getTotalCount()).isEqualTo(10L);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(100_000L);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyWithdrawStatus ─── */

  @Test
  @DisplayName("getYearlyWithdrawStatus returns yearly status counts")
  void getYearlyWithdrawStatus(VertxTestContext ctx) {
    mockPool();

    when(row.getString("year")).thenReturn("2026");
    when(row.getValue("total_completed")).thenReturn(50);
    when(row.getValue("total_amount")).thenReturn(500_000L);
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearStatusWithdrawCardNumber.builder()
        .year(2026).status("completed").build();

    repo.getYearlyWithdrawStatus(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalCount()).isEqualTo(50L);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(500_000L);
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyStatusByCard ─── */

  @Test
  @DisplayName("getMonthlyStatusByCard returns monthly status counts for card")
  void getMonthlyStatusByCard(VertxTestContext ctx) {
    mockPool();

    when(row.getString("year")).thenReturn("2026");
    when(row.getString("month")).thenReturn("Jun");
    when(row.getValue("total_failed")).thenReturn(3);
    when(row.getValue("total_amount")).thenReturn(50_000L);
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = MonthStatusWithdrawCardNumber.builder()
        .cardNumber("4111111111111111")
        .year(2026).month(6).status("failed").build();

    repo.getMonthlyStatusByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getMonth()).isEqualTo("Jun");
          assertThat(list.get(0).getTotalCount()).isEqualTo(3L);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(50_000L);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyStatusByCard ─── */

  @Test
  @DisplayName("getYearlyStatusByCard returns yearly status counts for card")
  void getYearlyStatusByCard(VertxTestContext ctx) {
    mockPool();

    when(row.getString("year")).thenReturn("2026");
    when(row.getValue("total_failed")).thenReturn(8);
    when(row.getValue("total_amount")).thenReturn(200_000L);
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearStatusWithdrawCardNumber.builder()
        .cardNumber("4111111111111111")
        .year(2026).status("failed").build();

    repo.getYearlyStatusByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalCount()).isEqualTo(8L);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(200_000L);
          ctx.completeNow();
        })));
  }
}
