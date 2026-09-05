package io.example.withdraw.repository.impl;

import io.example.withdraw.domain.requests.CreateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateWithdrawStatus;
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
class WithdrawCommandRepositoryImplTest {

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

  private WithdrawCommandRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new WithdrawCommandRepositoryImpl(pool);
  }

  private void mockPool() {
    lenient().when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockWithdrawRow() {
    lenient().when(row.getInteger("withdraw_id")).thenReturn(1);
    lenient().when(row.getUUID("withdraw_no")).thenReturn(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    lenient().when(row.getString("card_number")).thenReturn("4111111111111111");
    lenient().when(row.getLong("withdraw_amount")).thenReturn(50000L);
    lenient().when(row.getString("status")).thenReturn("completed");
    lenient().when(row.getString("idempotency_key")).thenReturn("withdraw-idem-test");
    lenient().when(row.getLocalDateTime("withdraw_time")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
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

  /* ─── createWithdraw ─── */

  @Test
  @DisplayName("createWithdraw inserts and returns new withdraw")
  void createWithdrawSuccess(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = CreateWithdrawRequest.builder()
        .cardNumber("4111111111111111")
        .withdrawAmount(50000)
        .idempotencyKey("withdraw-idem-test")
        .build();

    repo.createWithdraw(req, 10_000_000L)
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w).isNotNull();
          assertThat(w.getId()).isEqualTo(1);
          assertThat(w.getCardNumber()).isEqualTo("4111111111111111");
          assertThat(w.getWithdrawAmount()).isEqualTo(50000L);
          assertThat(w.getWithdrawNo()).isEqualTo("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByIdempotencyKey returns matching withdrawal")
  void findByIdempotencyKey(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRow();
    stubSingleRow();
    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByIdempotencyKey("withdraw-idem-test")
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w.getId()).isEqualTo(1);
          assertThat(w.getIdempotencyKey()).isEqualTo("withdraw-idem-test");
          ctx.completeNow();
        })));
  }

  /* ─── updateWithdraw ─── */

  @Test
  @DisplayName("updateWithdraw updates and returns withdraw")
  void updateWithdrawFound(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateWithdrawRequest.builder()
        .withdrawId(1)
        .cardNumber("4111111111111111")
        .withdrawAmount(75000)
        .build();

    repo.updateWithdraw(req, 10_000_000L)
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w).isNotNull();
          assertThat(w.getId()).isEqualTo(1);
          assertThat(w.getWithdrawAmount()).isEqualTo(50000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateWithdraw returns null when withdraw not found")
  void updateWithdrawNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateWithdrawRequest.builder()
        .withdrawId(99)
        .cardNumber("4111111111111111")
        .withdrawAmount(75000)
        .build();

    repo.updateWithdraw(req, 10_000_000L)
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── updateWithdrawStatus ─── */

  @Test
  @DisplayName("updateWithdrawStatus updates status and returns withdraw")
  void updateWithdrawStatusSuccess(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateWithdrawStatus.builder()
        .withdrawId(1)
        .status("completed")
        .build();

    repo.updateWithdrawStatus(req)
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w).isNotNull();
          assertThat(w.getId()).isEqualTo(1);
          assertThat(w.getStatus()).isEqualTo("completed");
          ctx.completeNow();
        })));
  }

  /* ─── trashWithdraw ─── */

  @Test
  @DisplayName("trashWithdraw soft-deletes and returns withdraw")
  void trashWithdrawFound(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashWithdraw(1)
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w).isNotNull();
          assertThat(w.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashWithdraw returns null when withdraw not found")
  void trashWithdrawNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashWithdraw(99)
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── restoreWithdraw ─── */

  @Test
  @DisplayName("restoreWithdraw restores and returns withdraw")
  void restoreWithdrawSuccess(VertxTestContext ctx) {
    mockPool();
    mockWithdrawRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreWithdraw(1)
        .onComplete(ctx.succeeding(w -> ctx.verify(() -> {
          assertThat(w).isNotNull();
          assertThat(w.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── deleteWithdrawPermanently ─── */

  @Test
  @DisplayName("deleteWithdrawPermanently returns true when rows deleted")
  void deleteWithdrawPermanentTrue(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(1);

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteWithdrawPermanently(1)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteWithdrawPermanently returns false when no rows affected")
  void deleteWithdrawPermanentFalse(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(0);

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteWithdrawPermanently(99)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isFalse();
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllWithdraws ─── */

  @Test
  @DisplayName("restoreAllWithdraws returns count of restored withdraws")
  void restoreAllWithdrawsSuccess(VertxTestContext ctx) {
    lenient().when(pool.query(anyString())).thenReturn(preparedQuery);
    stubDeleteResult(3);
    lenient().when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreAllWithdraws()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(3);
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllPermanentWithdraws ─── */

  @Test
  @DisplayName("deleteAllPermanentWithdraws returns count of deleted withdraws")
  void deleteAllPermanentWithdrawsSuccess(VertxTestContext ctx) {
    lenient().when(pool.query(anyString())).thenReturn(preparedQuery);
    stubDeleteResult(2);
    lenient().when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteAllPermanentWithdraws()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(2);
          ctx.completeNow();
        })));
  }
}
