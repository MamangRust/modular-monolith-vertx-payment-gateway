package io.example.withdraw.repository.impl;

import io.example.withdraw.domain.requests.FindAllWithdraws;
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
class WithdrawQueryRepositoryImplTest {

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

  private WithdrawQueryRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new WithdrawQueryRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockWithdrawRow() {
    when(row.getInteger("withdraw_id")).thenReturn(1);
    when(row.getUUID("withdraw_no")).thenReturn(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    when(row.getString("card_number")).thenReturn("4111111111111111");
    when(row.getLong("withdraw_amount")).thenReturn(50000L);
    when(row.getString("status")).thenReturn("completed");
    when(row.getLocalDateTime("withdraw_time")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("deleted_at")).thenReturn(null);
  }

  private void mockWithdrawRowWithCount() {
    mockWithdrawRow();
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

  /* ─── getWithdraws ─── */

  @Test
  @DisplayName("getWithdraws returns paged withdraws when found")
  void getWithdrawsFound(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = new FindAllWithdraws();
    repo.getWithdraws(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          assertThat(result.getData().get(0).getId()).isEqualTo(1);
          assertThat(result.getData().get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getWithdraws returns empty paged result when none found")
  void getWithdrawsEmpty(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = new FindAllWithdraws();
    repo.getWithdraws(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).isEmpty();
          assertThat(result.getTotalRecords()).isZero();
          ctx.completeNow();
        })));
  }

  /* ─── getActiveWithdraws ─── */

  @Test
  @DisplayName("getActiveWithdraws returns paged active withdraws")
  void getActiveWithdrawsFound(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = new FindAllWithdraws();
    repo.getActiveWithdraws(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedWithdraws ─── */

  @Test
  @DisplayName("getTrashedWithdraws returns paged trashed withdraws")
  void getTrashedWithdrawsFound(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = new FindAllWithdraws();
    repo.getTrashedWithdraws(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getWithdrawById ─── */

  @Test
  @DisplayName("getWithdrawById returns withdraw when found")
  void getWithdrawByIdFound(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRow();
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getWithdrawById(1)
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w).isNotNull();
          assertThat(w.getId()).isEqualTo(1);
          assertThat(w.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getWithdrawById returns null when not found")
  void getWithdrawByIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getWithdrawById(99)
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── findByTrashed ─── */

  @Test
  @DisplayName("findByTrashed returns trashed withdraw when found")
  void findByTrashedFound(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRow();
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashed(1)
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w).isNotNull();
          assertThat(w.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashed returns null when not found")
  void findByTrashedNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashed(99)
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── getWithdrawsByCardNumber ─── */

  @Test
  @DisplayName("getWithdrawsByCardNumber returns paged withdraws for card")
  void getWithdrawsByCardNumberFound(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getWithdrawsByCardNumber("4111111111111111", "completed", 1, 10)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          assertThat(result.getData().get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── getWithdrawsByCardNumberPrimitive ─── */

  @Test
  @DisplayName("getWithdrawsByCardNumberPrimitive returns list of withdraws for card")
  void getWithdrawsByCardNumberPrimitiveFound(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRow();
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getWithdrawsByCardNumberPrimitive("4111111111111111")
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }
}
