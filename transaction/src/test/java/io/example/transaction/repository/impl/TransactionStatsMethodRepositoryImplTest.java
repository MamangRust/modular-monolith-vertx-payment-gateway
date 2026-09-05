package io.example.transaction.repository.impl;

import io.example.transaction.domain.requests.YearCardNumberTransactionRequest;
import io.example.transaction.domain.requests.YearTransactionRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TransactionStatsMethodRepositoryImplTest {

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

  private TransactionStatsMethodRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TransactionStatsMethodRepositoryImpl(pool);
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
  @DisplayName("getMonthlyMethods returns monthly transaction methods")
  void monthlySuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getString("month")).thenReturn("Jan");
    when(row.getString("payment_method")).thenReturn("credit_card");
    when(row.getValue("total_transactions")).thenReturn(5);
    when(row.getValue("total_amount")).thenReturn(50000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearTransactionRequest.builder().year(2026).build();

    repo.getMonthlyMethods(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo("Jan");
          assertThat(list.get(0).getPaymentMethod()).isEqualTo("credit_card");
          assertThat(list.get(0).getTotalTransactions()).isEqualTo(5);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(50000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyMethods returns yearly transaction methods")
  void yearlySuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getValue("year")).thenReturn(2025);
    when(row.getString("payment_method")).thenReturn("debit_card");
    when(row.getValue("total_transactions")).thenReturn(12);
    when(row.getValue("total_amount")).thenReturn(120000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearTransactionRequest.builder().year(2026).build();

    repo.getYearlyMethods(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2025");
          assertThat(list.get(0).getPaymentMethod()).isEqualTo("debit_card");
          assertThat(list.get(0).getTotalTransactions()).isEqualTo(12);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(120000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyMethodsByCard returns monthly transaction methods filtered by card")
  void monthlyByCardSuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getString("month")).thenReturn("Mar");
    when(row.getString("payment_method")).thenReturn("credit_card");
    when(row.getValue("total_transactions")).thenReturn(3);
    when(row.getValue("total_amount")).thenReturn(30000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111")
        .year(2026)
        .build();

    repo.getMonthlyMethodsByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo("Mar");
          assertThat(list.get(0).getPaymentMethod()).isEqualTo("credit_card");
          assertThat(list.get(0).getTotalTransactions()).isEqualTo(3);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(30000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyMethodsByCard returns yearly transaction methods filtered by card")
  void yearlyByCardSuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getValue("year")).thenReturn(2026);
    when(row.getString("payment_method")).thenReturn("credit_card");
    when(row.getValue("total_transactions")).thenReturn(8);
    when(row.getValue("total_amount")).thenReturn(80000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111")
        .year(2026)
        .build();

    repo.getYearlyMethodsByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo("2026");
          assertThat(list.get(0).getPaymentMethod()).isEqualTo("credit_card");
          assertThat(list.get(0).getTotalTransactions()).isEqualTo(8);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(80000L);
          ctx.completeNow();
        })));
  }
}
