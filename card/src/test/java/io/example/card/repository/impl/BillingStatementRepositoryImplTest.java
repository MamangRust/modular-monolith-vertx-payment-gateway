package io.example.card.repository.impl;

import io.example.card.model.BillingStatement;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingStatementRepositoryImplTest {

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

  private BillingStatementRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new BillingStatementRepositoryImpl(pool);
  }

  private void mockPool() {
    lenient().when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockStmtRow() {
    lenient().when(row.getInteger("statement_id")).thenReturn(1);
    lenient().when(row.getString("card_number")).thenReturn("4111111111111111");
    lenient().when(row.getLocalDate("statement_date")).thenReturn(LocalDate.of(2026, 6, 26));
    lenient().when(row.getLocalDate("due_date")).thenReturn(LocalDate.of(2026, 7, 26));
    lenient().when(row.getLong("opening_balance")).thenReturn(100000L);
    lenient().when(row.getLong("purchases")).thenReturn(50000L);
    lenient().when(row.getLong("cash_advances")).thenReturn(0L);
    lenient().when(row.getLong("payments")).thenReturn(100000L);
    lenient().when(row.getLong("fees")).thenReturn(0L);
    lenient().when(row.getLong("interest_charged")).thenReturn(500L);
    lenient().when(row.getLong("closing_balance")).thenReturn(50500L);
    lenient().when(row.getLong("minimum_payment")).thenReturn(10000L);
    lenient().when(row.getString("payment_status")).thenReturn("UNPAID");
    lenient().when(rowSet.iterator()).thenReturn(iterator);
  }

  private void stubSingleRow() {
    lenient().when(iterator.hasNext()).thenReturn(true, false);
    lenient().when(iterator.next()).thenReturn(row);
    lenient().doAnswer(invocation -> {
      java.util.function.Consumer<Row> consumer = invocation.getArgument(0);
      consumer.accept(row);
      return null;
    }).when(rowSet).forEach(any(java.util.function.Consumer.class));
  }

  private void stubNoRows() {
    lenient().when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(iterator.hasNext()).thenReturn(false);
  }

  @Test
  @DisplayName("insertStatement inserts statement and returns it")
  void insertStatementSuccess(VertxTestContext ctx) {
    mockPool();
    mockStmtRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    BillingStatement stmt = BillingStatement.builder()
        .cardNumber("4111111111111111")
        .statementDate(LocalDate.of(2026, 6, 26))
        .dueDate(LocalDate.of(2026, 7, 26))
        .openingBalance(100000L)
        .purchases(50000L)
        .cashAdvances(0L)
        .payments(100000L)
        .fees(0L)
        .interestCharged(500L)
        .closingBalance(50500L)
        .minimumPayment(10000L)
        .build();

    repo.insertStatement(stmt)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getStatementId()).isEqualTo(1);
          assertThat(result.getPaymentStatus()).isEqualTo("UNPAID");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByCardAndCycle returns statement if found")
  void findByCardAndCycleSuccess(VertxTestContext ctx) {
    mockPool();
    mockStmtRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByCardAndCycle("4111111111111111", LocalDate.of(2026, 6, 26))
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          assertThat(result.getStatementDate()).isEqualTo(LocalDate.of(2026, 6, 26));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByCardNumber returns list of statements")
  void findByCardNumberSuccess(VertxTestContext ctx) {
    mockPool();
    mockStmtRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByCardNumber("4111111111111111", 10, 0)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updatePaymentStatus updates status and returns statement")
  void updatePaymentStatusSuccess(VertxTestContext ctx) {
    mockPool();
    mockStmtRow();
    lenient().when(row.getString("payment_status")).thenReturn("PAID");
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.updatePaymentStatus(1, "PAID")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getStatementId()).isEqualTo(1);
          assertThat(result.getPaymentStatus()).isEqualTo("PAID");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findLatestByCardNumber returns latest statement")
  void findLatestSuccess(VertxTestContext ctx) {
    mockPool();
    mockStmtRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findLatestByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("countByCardNumber returns statement count")
  void countByCardNumberSuccess(VertxTestContext ctx) {
    mockPool();
    lenient().when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(iterator.next()).thenReturn(row);
    lenient().when(row.getInteger("cnt")).thenReturn(3);
    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.countByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(3);
          ctx.completeNow();
        })));
  }
}
