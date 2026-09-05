package io.example.transfer.repository.impl;

import io.example.transfer.domain.requests.FindAllTransfers;
import io.example.transfer.model.Transfer;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TransferQueryRepositoryImplTest {

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

  private TransferQueryRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TransferQueryRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockTransferRow() {
    when(row.getInteger("transfer_id")).thenReturn(1);
    when(row.getUUID("transfer_no")).thenReturn(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    when(row.getString("transfer_from")).thenReturn("4111111111111111");
    when(row.getString("transfer_to")).thenReturn("5111111111111111");
    when(row.getLong("transfer_amount")).thenReturn(50000L);
    when(row.getString("idempotency_key")).thenReturn("idem-key-123");
    when(row.getString("status")).thenReturn("completed");
    when(row.getLocalDateTime("transfer_time")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("deleted_at")).thenReturn(null);
  }

  private void mockTransferRowWithCount() {
    mockTransferRow();
    when(row.getValue("total_count")).thenReturn(1);
  }

  private void stubPagedRows() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
  }

  private void stubNoRows() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);
  }

  /* ─── getTransfers / getAllTransfers ─── */

  @Test
  @DisplayName("getTransfers returns paged transfers when found")
  void getTransfersFound(VertxTestContext ctx) {
    mockPool();
    mockTransferRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = new FindAllTransfers();
    repo.getTransfers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          assertThat(result.getData().get(0).getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransfers returns empty paged result when none found")
  void getTransfersEmpty(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = new FindAllTransfers();
    repo.getTransfers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).isEmpty();
          assertThat(result.getTotalRecords()).isZero();
          ctx.completeNow();
        })));
  }

  /* ─── getActiveTransfers ─── */

  @Test
  @DisplayName("getActiveTransfers returns paged active transfers")
  void getActiveTransfersFound(VertxTestContext ctx) {
    mockPool();
    mockTransferRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = new FindAllTransfers();
    repo.getActiveTransfers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedTransfers ─── */

  @Test
  @DisplayName("getTrashedTransfers returns paged trashed transfers")
  void getTrashedTransfersFound(VertxTestContext ctx) {
    mockPool();
    mockTransferRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = new FindAllTransfers();
    repo.getTrashedTransfers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getTransferById ─── */

  @Test
  @DisplayName("getTransferById returns transfer when found")
  void getTransferByIdFound(VertxTestContext ctx) {
    mockPool();
    mockTransferRow();
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getTransferById(1)
        .onComplete(ctx.succeeding(transfer -> ctx.verify(() -> {
          assertThat(transfer).isNotNull();
          assertThat(transfer.getId()).isEqualTo(1);
          assertThat(transfer.getTransferFrom()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransferById returns null when not found")
  void getTransferByIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getTransferById(99)
        .onComplete(ctx.succeeding(transfer -> ctx.verify(() -> {
          assertThat(transfer).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── getTransfersBySender (getTransferByTransferFrom) ─── */

  @Test
  @DisplayName("getTransfersBySender returns list of transfers sent from card")
  void getTransfersBySenderFound(VertxTestContext ctx) {
    mockPool();
    mockTransferRow();
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getTransfersBySender("4111111111111111")
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getTransferFrom()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── getTransfersByReceiver (getTransferByTransferTo) ─── */

  @Test
  @DisplayName("getTransfersByReceiver returns list of transfers received to card")
  void getTransfersByReceiverFound(VertxTestContext ctx) {
    mockPool();
    mockTransferRow();
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getTransfersByReceiver("5111111111111111")
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getTransferTo()).isEqualTo("5111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── findByTrashedId ─── */

  @Test
  @DisplayName("findByTrashedId returns trashed transfer when found")
  void findByTrashedIdFound(VertxTestContext ctx) {
    mockPool();
    mockTransferRow();
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashedId(1)
        .onComplete(ctx.succeeding(transfer -> ctx.verify(() -> {
          assertThat(transfer).isNotNull();
          assertThat(transfer.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashedId returns null when not found")
  void findByTrashedIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashedId(99)
        .onComplete(ctx.succeeding(transfer -> ctx.verify(() -> {
          assertThat(transfer).isNull();
          ctx.completeNow();
        })));
  }
}
