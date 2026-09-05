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
class TransactionStatsAmountRepositoryImplTest {

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

  private TransactionStatsAmountRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TransactionStatsAmountRepositoryImpl(pool);
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
  @DisplayName("getMonthlyAmounts returns monthly transaction amounts")
  void monthlySuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getString("month")).thenReturn("Jan");
    when(row.getValue("total_amount")).thenReturn(10000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearTransactionRequest.builder().year(2026).build();

    repo.getMonthlyAmounts(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo("Jan");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(10000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyAmounts returns yearly transaction amounts")
  void yearlySuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getValue("year")).thenReturn(2025);
    when(row.getValue("total_amount")).thenReturn(120000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearTransactionRequest.builder().year(2026).build();

    repo.getYearlyAmounts(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(120000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyAmountsByCard returns monthly transaction amounts filtered by card")
  void monthlyByCardSuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getString("month")).thenReturn("Feb");
    when(row.getValue("total_amount")).thenReturn(25000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111")
        .year(2026)
        .build();

    repo.getMonthlyAmountsByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo("Feb");
          assertThat(list.get(0).getTotalAmount()).isEqualTo(25000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyAmountsByCard returns yearly transaction amounts filtered by card")
  void yearlyByCardSuccess(VertxTestContext ctx) {
    mockPool();
    when(row.getValue("year")).thenReturn(2026);
    when(row.getValue("total_amount")).thenReturn(90000L);
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = YearCardNumberTransactionRequest.builder()
        .cardNumber("4111111111111111")
        .year(2026)
        .build();

    repo.getYearlyAmountsByCard(req)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getTotalAmount()).isEqualTo(90000L);
          ctx.completeNow();
        })));
  }
}
