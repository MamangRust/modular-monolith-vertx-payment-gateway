package io.example.transfer.repository.impl;

import io.example.transfer.domain.requests.MonthYearCardNumber;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferStatsByCardRepositoryImplTest {

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

  private TransferStatsByCardRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TransferStatsByCardRepositoryImpl(pool);
  }

  private void mockPool() {
    lenient().when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockMonthAmountRow() {
    lenient().when(row.getString("month")).thenReturn("Jan");
    lenient().when(row.getValue("total_transfer_amount")).thenReturn(50000L);
  }

  private void mockYearAmountRow() {
    lenient().when(row.getValue("year")).thenReturn(2026);
    lenient().when(row.getValue("total_transfer_amount")).thenReturn(60000L);
  }

  private void stubSingleRow() {
    lenient().when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(iterator.hasNext()).thenReturn(true, false);
    lenient().when(iterator.next()).thenReturn(row);
  }

  /* ─── getMonthlySenderAmountsByCard ─── */

  @Test
  @DisplayName("getMonthlySenderAmountsByCard returns monthly amounts sent by card")
  void monthlySentSuccess(VertxTestContext ctx) {
    mockPool();
    mockMonthAmountRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = MonthYearCardNumber.builder().cardNumber("4111111111111111").year(2026).build();
    repo.getMonthlySenderAmountsByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo("Jan");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(50000L);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlySenderAmountsByCard ─── */

  @Test
  @DisplayName("getYearlySenderAmountsByCard returns yearly amounts sent by card")
  void yearlySentSuccess(VertxTestContext ctx) {
    mockPool();
    mockYearAmountRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = MonthYearCardNumber.builder().cardNumber("4111111111111111").year(2026).build();
    repo.getYearlySenderAmountsByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(60000L);
          ctx.completeNow();
        })));
  }

  /* ─── getMonthlyReceiverAmountsByCard ─── */

  @Test
  @DisplayName("getMonthlyReceiverAmountsByCard returns monthly amounts received by card")
  void monthlyReceivedSuccess(VertxTestContext ctx) {
    mockPool();
    mockMonthAmountRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = MonthYearCardNumber.builder().cardNumber("5111111111111111").year(2026).build();
    repo.getMonthlyReceiverAmountsByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo("Jan");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(50000L);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyReceiverAmountsByCard ─── */

  @Test
  @DisplayName("getYearlyReceiverAmountsByCard returns yearly amounts received by card")
  void yearlyReceivedSuccess(VertxTestContext ctx) {
    mockPool();
    mockYearAmountRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = MonthYearCardNumber.builder().cardNumber("5111111111111111").year(2026).build();
    repo.getYearlyReceiverAmountsByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(60000L);
          ctx.completeNow();
        })));
  }
}
