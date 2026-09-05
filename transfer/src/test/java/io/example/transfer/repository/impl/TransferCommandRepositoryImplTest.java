package io.example.transfer.repository.impl;

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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferCommandRepositoryImplTest {

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

  private TransferCommandRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TransferCommandRepositoryImpl(pool);
  }

  private void mockPool() {
    lenient().when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockTransferRow() {
    lenient().when(row.getInteger("transfer_id")).thenReturn(1);
    lenient().when(row.getUUID("transfer_no")).thenReturn(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    lenient().when(row.getString("transfer_from")).thenReturn("4111111111111111");
    lenient().when(row.getString("transfer_to")).thenReturn("5111111111111111");
    lenient().when(row.getLong("transfer_amount")).thenReturn(50000L);
    lenient().when(row.getString("status")).thenReturn("completed");
    lenient().when(row.getLocalDateTime("transfer_time")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    lenient().when(row.getLocalDateTime("created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    lenient().when(row.getLocalDateTime("updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    lenient().when(row.getLocalDateTime("deleted_at")).thenReturn(null);
  }

  private void stubSingleRow() {
    lenient().when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(iterator.hasNext()).thenReturn(true, false);
    lenient().when(iterator.next()).thenReturn(row);
  }

  private void stubNoRows() {
    lenient().when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(iterator.hasNext()).thenReturn(false);
  }

  private void stubDeleteResult(int count) {
    lenient().when(rowSet.rowCount()).thenReturn(count);
  }

  /* ─── createTransfer ─── */

  @Test
  @DisplayName("createTransfer inserts and returns new transfer")
  void createTransferSuccess(VertxTestContext ctx) {
    mockPool();
    mockTransferRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.createTransfer("4111111111111111", "5111111111111111", 50000L, "idem-key-123")
        .onComplete(ctx.succeeding(transfer -> ctx.verify(() -> {
          assertThat(transfer).isNotNull();
          assertThat(transfer.getId()).isEqualTo(1);
          assertThat(transfer.getTransferFrom()).isEqualTo("4111111111111111");
          assertThat(transfer.getTransferTo()).isEqualTo("5111111111111111");
          assertThat(transfer.getTransferAmount()).isEqualTo(50000L);
          ctx.completeNow();
        })));
  }

  /* ─── updateTransfer ─── */

  @Test
  @DisplayName("updateTransfer updates and returns transfer")
  void updateTransferFound(VertxTestContext ctx) {
    mockPool();
    mockTransferRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.updateTransfer(1, "4111111111111111", "5111111111111111", 75000L)
        .onComplete(ctx.succeeding(transfer -> ctx.verify(() -> {
          assertThat(transfer).isNotNull();
          assertThat(transfer.getId()).isEqualTo(1);
          assertThat(transfer.getTransferAmount()).isEqualTo(50000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateTransfer returns null when transfer not found")
  void updateTransferNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.updateTransfer(99, "4111111111111111", "5111111111111111", 75000L)
        .onComplete(ctx.succeeding(transfer -> ctx.verify(() -> {
          assertThat(transfer).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── trashTransfer ─── */

  @Test
  @DisplayName("trashTransfer soft-deletes and returns transfer")
  void trashTransferFound(VertxTestContext ctx) {
    mockPool();
    mockTransferRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashTransfer(1)
        .onComplete(ctx.succeeding(transfer -> ctx.verify(() -> {
          assertThat(transfer).isNotNull();
          assertThat(transfer.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashTransfer returns null when transfer not found")
  void trashTransferNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashTransfer(99)
        .onComplete(ctx.succeeding(transfer -> ctx.verify(() -> {
          assertThat(transfer).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── restoreTransfer ─── */

  @Test
  @DisplayName("restoreTransfer restores and returns transfer")
  void restoreTransferSuccess(VertxTestContext ctx) {
    mockPool();
    mockTransferRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreTransfer(1)
        .onComplete(ctx.succeeding(transfer -> ctx.verify(() -> {
          assertThat(transfer).isNotNull();
          assertThat(transfer.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── deleteTransferPermanently ─── */

  @Test
  @DisplayName("deleteTransferPermanently returns true when rows deleted")
  void deleteTransferPermanentTrue(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(1);

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteTransferPermanently(1)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteTransferPermanently returns false when no rows affected")
  void deleteTransferPermanentFalse(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(0);

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteTransferPermanently(99)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isFalse();
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllTransfers ─── */

  @Test
  @DisplayName("restoreAllTransfers returns count of restored transfers")
  void restoreAllTransfersSuccess(VertxTestContext ctx) {
    lenient().when(pool.query(anyString())).thenReturn(preparedQuery);
    stubDeleteResult(3);
    lenient().when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreAllTransfers()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(3);
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllPermanentTransfers ─── */

  @Test
  @DisplayName("deleteAllPermanentTransfers returns count of deleted transfers")
  void deleteAllPermanentTransfersSuccess(VertxTestContext ctx) {
    lenient().when(pool.query(anyString())).thenReturn(preparedQuery);
    stubDeleteResult(2);
    lenient().when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteAllPermanentTransfers()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(2);
          ctx.completeNow();
        })));
  }
}
