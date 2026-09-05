package io.example.card.repository.impl;

import io.example.card.model.CardAuthTransaction;
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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardAuthTransactionRepositoryImplTest {

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

  private CardAuthTransactionRepositoryImpl repo;
  private final UUID txnUuid = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    repo = new CardAuthTransactionRepositoryImpl(pool);
  }

  private void mockPool() {
    lenient().when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockTxnRow() {
    lenient().when(row.getUUID("txn_id")).thenReturn(txnUuid);
    lenient().when(row.getString("card_number")).thenReturn("4111111111111111");
    lenient().when(row.getInteger("merchant_id")).thenReturn(123);
    lenient().when(row.getLong("amount")).thenReturn(1000L);
    lenient().when(row.getString("currency")).thenReturn("IDR");
    lenient().when(row.getString("status")).thenReturn("PENDING");
    lenient().when(row.getString("pos_entry_mode")).thenReturn("01");
    lenient().when(row.getString("mcc")).thenReturn("5411");
    lenient().when(row.getString("idempotency_key")).thenReturn("idem-123");
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
  @DisplayName("insertPending inserts transaction and returns it")
  void insertPendingSuccess(VertxTestContext ctx) {
    mockPool();
    mockTxnRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    CardAuthTransaction txn = CardAuthTransaction.builder()
        .cardNumber("4111111111111111")
        .merchantId(123)
        .amount(1000L)
        .currency("IDR")
        .posEntryMode("01")
        .mcc("5411")
        .idempotencyKey("idem-123")
        .build();

    repo.insertPending(txn)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getTxnId()).isEqualTo(txnUuid);
          assertThat(result.getStatus()).isEqualTo("PENDING");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("approve updates status to APPROVED and returns updated txn")
  void approveSuccess(VertxTestContext ctx) {
    mockPool();
    mockTxnRow();
    lenient().when(row.getString("status")).thenReturn("APPROVED");
    lenient().when(row.getString("auth_code")).thenReturn("AUTH123");
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.approve(txnUuid.toString(), "AUTH123")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getStatus()).isEqualTo("APPROVED");
          assertThat(result.getAuthCode()).isEqualTo("AUTH123");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("decline updates status to DECLINED and returns updated txn")
  void declineSuccess(VertxTestContext ctx) {
    mockPool();
    mockTxnRow();
    lenient().when(row.getString("status")).thenReturn("DECLINED");
    lenient().when(row.getString("decline_code")).thenReturn("51");
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.decline(txnUuid.toString(), "51")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getStatus()).isEqualTo("DECLINED");
          assertThat(result.getDeclineCode()).isEqualTo("51");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("reverse updates status to REVERSED and returns updated txn")
  void reverseSuccess(VertxTestContext ctx) {
    mockPool();
    mockTxnRow();
    lenient().when(row.getString("status")).thenReturn("REVERSED");
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.reverse(txnUuid.toString())
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getStatus()).isEqualTo("REVERSED");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByIdempotencyKey returns txn if found")
  void findByIdempotencyKeySuccess(VertxTestContext ctx) {
    mockPool();
    mockTxnRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByIdempotencyKey("idem-123")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getIdempotencyKey()).isEqualTo("idem-123");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findById returns txn if found")
  void findByIdSuccess(VertxTestContext ctx) {
    mockPool();
    mockTxnRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findById(txnUuid.toString())
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getTxnId()).isEqualTo(txnUuid);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByCardNumber returns list of txns")
  void findByCardNumberSuccess(VertxTestContext ctx) {
    mockPool();
    mockTxnRow();
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
  @DisplayName("countRecentByCardNumber returns recent count")
  void countRecentSuccess(VertxTestContext ctx) {
    mockPool();
    lenient().when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(iterator.next()).thenReturn(row);
    lenient().when(row.getLong("cnt")).thenReturn(5L);
    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.countRecentByCardNumber("4111111111111111", 60)
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(5L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateRiskScore updates and returns row count")
  void updateRiskScoreSuccess(VertxTestContext ctx) {
    mockPool();
    lenient().when(rowSet.rowCount()).thenReturn(1);
    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.updateRiskScore(txnUuid.toString(), 15)
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
