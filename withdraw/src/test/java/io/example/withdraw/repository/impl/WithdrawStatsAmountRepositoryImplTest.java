package io.example.withdraw.repository.impl;

import io.example.withdraw.domain.requests.YearMonthCardNumber;
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
class WithdrawStatsAmountRepositoryImplTest {

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

  private WithdrawStatsAmountRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new WithdrawStatsAmountRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockMonthAmountRow() {
    when(row.getString("month")).thenReturn("Jan");
    when(row.getValue("total_withdraw_amount")).thenReturn(50000L);
  }

  private void mockYearAmountRow() {
    when(row.getValue("year")).thenReturn(2026);
    when(row.getValue("total_withdraw_amount")).thenReturn(60000L);
  }

  private void stubSingleRow() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
  }

  /* ─── getMonthlyWithdrawAmounts ─── */

  @Test
  @DisplayName("getMonthlyWithdrawAmounts returns monthly amounts list")
  void getMonthlyWithdrawAmountsSuccess(VertxTestContext ctx) {
    mockPool();
    mockMonthAmountRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getMonthlyWithdrawAmounts(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo("Jan");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(50000L);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyWithdrawAmounts ─── */

  @Test
  @DisplayName("getYearlyWithdrawAmounts returns yearly amounts list")
  void getYearlyWithdrawAmountsSuccess(VertxTestContext ctx) {
    mockPool();
    mockYearAmountRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getYearlyWithdrawAmounts(2026)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(60000L);
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyWithdrawAmountsByCard ─── */

  @Test
  @DisplayName("getMonthlyWithdrawAmountsByCard returns monthly amounts for card")
  void getMonthlyWithdrawAmountsByCardSuccess(VertxTestContext ctx) {
    mockPool();
    mockMonthAmountRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearMonthCardNumber.builder()
        .cardNumber("4111111111111111")
        .year(2026)
        .build();

    repo.getMonthlyWithdrawAmountsByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo("Jan");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(50000L);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyWithdrawAmountsByCard ─── */

  @Test
  @DisplayName("getYearlyWithdrawAmountsByCard returns yearly amounts for card")
  void getYearlyWithdrawAmountsByCardSuccess(VertxTestContext ctx) {
    mockPool();
    mockYearAmountRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearMonthCardNumber.builder()
        .cardNumber("4111111111111111")
        .year(2026)
        .build();

    repo.getYearlyWithdrawAmountsByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(60000L);
          ctx.completeNow();
        })));
  }
}
