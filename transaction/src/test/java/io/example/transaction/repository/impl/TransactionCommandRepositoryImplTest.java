package io.example.transaction.repository.impl;

import io.example.transaction.model.Transaction;
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
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionCommandRepositoryImplTest {

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

  private TransactionCommandRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TransactionCommandRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockTransactionRow() {
    when(row.getInteger("transaction_id")).thenReturn(1);
    when(row.getInteger("id")).thenReturn(null);
    when(row.getUUID("transaction_no")).thenReturn(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    when(row.getString("transaction_no")).thenReturn(null);
    when(row.getString("card_number")).thenReturn("4111111111111111");
    when(row.getLong("amount")).thenReturn(50000L);
    when(row.getString("payment_method")).thenReturn("credit_card");
    when(row.getInteger("merchant_id")).thenReturn(42);
    when(row.getString("status")).thenReturn("pending");
    when(row.getString("idempotency_key")).thenReturn("txn-idem-123");
    when(row.getLocalDateTime("transaction_time")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("deleted_at")).thenReturn(null);
    when(rowSet.iterator()).thenReturn(iterator);
  }

  private void stubSingleRow() {
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
  }

  private void stubNoRows() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);
  }

  private void stubDeleteResult(int count) {
    when(rowSet.rowCount()).thenReturn(count);
  }

  /* ─── createTransaction ─── */

  @Test
  @DisplayName("createTransaction inserts and returns new transaction")
  void createTransactionSuccess(VertxTestContext ctx) {
    mockPool();
    mockTransactionRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = CreateTransactionRequest.newBuilder()
        .setCardNumber("4111111111111111")
        .setAmount(50000)
        .setPaymentMethod("credit_card")
        .setMerchantId(42)
        .setIdempotencyKey("txn-idem-123")
        .build();

    repo.createTransaction(req)
        .onComplete(ctx.succeeding(txn -> ctx.verify(() -> {
          assertThat(txn).isNotNull();
          assertThat(txn.getId()).isEqualTo(1);
          assertThat(txn.getCardNumber()).isEqualTo("4111111111111111");
          assertThat(txn.getAmount()).isEqualTo(50000L);
          assertThat(txn.getPaymentMethod()).isEqualTo("credit_card");
          assertThat(txn.getMerchantId()).isEqualTo(42);
          assertThat(txn.getStatus()).isEqualTo("pending");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByIdempotencyKey returns the existing transaction")
  void findByIdempotencyKeySuccess(VertxTestContext ctx) {
    mockPool();
    mockTransactionRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByIdempotencyKey("txn-idem-123")
        .onComplete(ctx.succeeding(txn -> ctx.verify(() -> {
          assertThat(txn).isNotNull();
          assertThat(txn.getIdempotencyKey()).isEqualTo("txn-idem-123");
          ctx.completeNow();
        })));
  }

  /* ─── updateTransaction ─── */

  @Test
  @DisplayName("updateTransaction updates and returns transaction")
  void updateTransactionSuccess(VertxTestContext ctx) {
    mockPool();
    mockTransactionRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateTransactionRequest.newBuilder()
        .setTransactionId(1)
        .setCardNumber("4111111111111111")
        .setAmount(75000)
        .setPaymentMethod("debit_card")
        .setMerchantId(42)
        .build();

    repo.updateTransaction(req)
        .onComplete(ctx.succeeding(txn -> ctx.verify(() -> {
          assertThat(txn).isNotNull();
          assertThat(txn.getId()).isEqualTo(1);
          assertThat(txn.getAmount()).isEqualTo(50000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateTransaction returns null when transaction not found")
  void updateTransactionNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateTransactionRequest.newBuilder().setTransactionId(99).build();

    repo.updateTransaction(req)
        .onComplete(ctx.succeeding(txn -> ctx.verify(() -> {
          assertThat(txn).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── trashTransaction ─── */

  @Test
  @DisplayName("trashTransaction soft-deletes and returns transaction")
  void trashTransactionSuccess(VertxTestContext ctx) {
    mockPool();
    mockTransactionRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashed(1)
        .onComplete(ctx.succeeding(txn -> ctx.verify(() -> {
          assertThat(txn).isNotNull();
          assertThat(txn.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashTransaction returns null when transaction not found")
  void trashTransactionNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashed(99)
        .onComplete(ctx.succeeding(txn -> ctx.verify(() -> {
          assertThat(txn).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── restoreTransaction ─── */

  @Test
  @DisplayName("restoreTransaction restores and returns transaction")
  void restoreTransactionSuccess(VertxTestContext ctx) {
    mockPool();
    mockTransactionRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreTransaction(1)
        .onComplete(ctx.succeeding(txn -> ctx.verify(() -> {
          assertThat(txn).isNotNull();
          assertThat(txn.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── deleteTransactionPermanently ─── */

  @Test
  @DisplayName("deleteTransactionPermanently deletes and returns true")
  void deleteTransactionPermanentlyTrue(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(1);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deletePermanently(1)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteTransactionPermanently returns false when no rows affected")
  void deleteTransactionPermanentlyFalse(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(0);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deletePermanently(99)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isFalse();
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllTransactions ─── */

  @Test
  @DisplayName("restoreAllTransactions returns count of restored transactions")
  void restoreAllTransactionsSuccess(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(preparedQuery);
    stubDeleteResult(5);

    when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreAllTransactions()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(5);
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllPermanentTransactions ─── */

  @Test
  @DisplayName("deleteAllPermanentTransactions returns count of permanently deleted transactions")
  void deleteAllPermanentTransactionsSuccess(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(preparedQuery);
    stubDeleteResult(3);

    when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteAllPermanentTransactions()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(3);
          ctx.completeNow();
        })));
  }
}
