package io.example.card.repository.impl;

import io.example.card.model.CardPayment;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardPaymentRepositoryImplTest {

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

  private CardPaymentRepositoryImpl repo;
  private final UUID paymentUuid = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    repo = new CardPaymentRepositoryImpl(pool);
  }

  private void mockPool() {
    lenient().when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockPaymentRow() {
    lenient().when(row.getUUID("payment_id")).thenReturn(paymentUuid);
    lenient().when(row.getString("reference_id")).thenReturn("ref-123");
    lenient().when(row.getString("card_number")).thenReturn("4111111111111111");
    lenient().when(row.getLong("amount")).thenReturn(50000L);
    lenient().when(row.getString("payment_channel")).thenReturn("BANK_TRANSFER");
    lenient().when(row.getString("status")).thenReturn("POSTED");
    lenient().when(row.getInteger("statement_id")).thenReturn(1);
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

  @Test
  @DisplayName("findByReferenceId returns payment if found")
  void findByReferenceIdSuccess(VertxTestContext ctx) {
    mockPool();
    mockPaymentRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByReferenceId("ref-123")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getReferenceId()).isEqualTo("ref-123");
          assertThat(result.getPaymentId()).isEqualTo(paymentUuid);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("insertPayment inserts and returns new payment")
  void insertPaymentSuccess(VertxTestContext ctx) {
    mockPool();
    mockPaymentRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    CardPayment payment = CardPayment.builder()
        .referenceId("ref-123")
        .cardNumber("4111111111111111")
        .amount(50000L)
        .paymentChannel("BANK_TRANSFER")
        .statementId(1)
        .build();

    repo.insertPayment(payment)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getReferenceId()).isEqualTo("ref-123");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByCardNumber returns history list")
  void findByCardNumberSuccess(VertxTestContext ctx) {
    mockPool();
    mockPaymentRow();
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
  @DisplayName("countByCardNumber returns payment count")
  void countByCardNumberSuccess(VertxTestContext ctx) {
    mockPool();
    lenient().when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(iterator.next()).thenReturn(row);
    lenient().when(row.getInteger("cnt")).thenReturn(4);
    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.countByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(4);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("totalPaymentsByCardNumber returns sum of payments")
  void totalPaymentsByCardNumberSuccess(VertxTestContext ctx) {
    mockPool();
    lenient().when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(iterator.next()).thenReturn(row);
    lenient().when(row.getLong("total")).thenReturn(200000L);
    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.totalPaymentsByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(total -> ctx.verify(() -> {
          assertThat(total).isEqualTo(200000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findById returns payment if found")
  void findByIdSuccess(VertxTestContext ctx) {
    mockPool();
    mockPaymentRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findById(paymentUuid.toString())
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getPaymentId()).isEqualTo(paymentUuid);
          ctx.completeNow();
        })));
  }
}
