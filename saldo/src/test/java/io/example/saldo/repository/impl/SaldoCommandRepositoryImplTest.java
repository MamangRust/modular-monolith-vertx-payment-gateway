package io.example.saldo.repository.impl;

import io.example.saldo.domain.requests.CreateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoBalanceRequest;
import io.example.saldo.domain.requests.UpdateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoWithdrawRequest;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class SaldoCommandRepositoryImplTest {

  @Mock
  private Pool pool;

  @Mock
  private PreparedQuery<RowSet<Row>> preparedQuery;

  @Mock
  private Query<RowSet<Row>> query;

  @Mock
  private RowSet<Row> rowSet;

  @Mock
  private RowIterator<Row> iterator;

  @Mock
  private Row row;

  private SaldoCommandRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new SaldoCommandRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockQuery() {
    when(pool.query(anyString())).thenReturn(query);
    when(query.execute()).thenReturn(Future.succeededFuture(rowSet));
  }

  private Timestamp now() {
    return Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
  }

  private void mockSaldoRow() {
    when(row.getInteger("saldo_id")).thenReturn(1);
    when(row.getString("card_number")).thenReturn("4111111111111111");
    when(row.getLong("total_balance")).thenReturn(1_000_000L);
    when(row.getLong("withdraw_amount")).thenReturn(null);
    when(row.get(LocalDateTime.class, "withdraw_time")).thenReturn(null);
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(null);
  }

  private void stubSingleRow() {
    when(rowSet.iterator()).thenReturn(iterator);
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

  /* ─── createSaldo ─── */

  @Test
  @DisplayName("createSaldo inserts and returns new saldo")
  void createSaldoSuccess(VertxTestContext ctx) {
    mockPool();
    mockSaldoRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = CreateSaldoRequest.builder()
        .cardNumber("4111111111111111")
        .totalBalance(1_000_000L)
        .build();

    repo.createSaldo(req)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNotNull();
          assertThat(saldo.getId()).isEqualTo(1);
          assertThat(saldo.getCardNumber()).isEqualTo("4111111111111111");
          assertThat(saldo.getTotalBalance()).isEqualTo(1_000_000L);
          ctx.completeNow();
        })));
  }

  /* ─── updateSaldo ─── */

  @Test
  @DisplayName("updateSaldo updates and returns saldo")
  void updateSaldoSuccess(VertxTestContext ctx) {
    mockPool();
    mockSaldoRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateSaldoRequest.builder()
        .saldoId(1)
        .cardNumber("4111111111111111")
        .totalBalance(2_000_000L)
        .build();

    repo.updateSaldo(req)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNotNull();
          assertThat(saldo.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateSaldo returns null when saldo not found")
  void updateSaldoNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateSaldoRequest.builder().saldoId(99).cardNumber("0000000000000000").totalBalance(1_000_000L).build();

    repo.updateSaldo(req)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── updateSaldoBalance ─── */

  @Test
  @DisplayName("updateSaldoBalance updates and returns saldo")
  void updateSaldoBalanceSuccess(VertxTestContext ctx) {
    mockPool();
    mockSaldoRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateSaldoBalanceRequest.builder()
        .cardNumber("4111111111111111")
        .totalBalance(500_000L)
        .build();

    repo.updateSaldoBalance(req)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNotNull();
          assertThat(saldo.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── updateSaldoWithdraw ─── */

  @Test
  @DisplayName("updateSaldoWithdraw updates and returns saldo")
  void updateSaldoWithdrawSuccess(VertxTestContext ctx) {
    mockPool();
    mockSaldoRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateSaldoWithdrawRequest.builder()
        .cardNumber("4111111111111111")
        .withdrawAmount(100_000L)
        .withdrawTime(LocalDateTime.of(2026, 6, 26, 10, 0))
        .build();

    repo.updateSaldoWithdraw(req)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNotNull();
          assertThat(saldo.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── trash ─── */

  @Test
  @DisplayName("trash soft-deletes and returns saldo")
  void trashSuccess(VertxTestContext ctx) {
    mockPool();
    mockSaldoRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trash(1)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNotNull();
          assertThat(saldo.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trash returns null when saldo not found")
  void trashNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trash(99)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── restore ─── */

  @Test
  @DisplayName("restore restores and returns saldo")
  void restoreSuccess(VertxTestContext ctx) {
    mockPool();
    mockSaldoRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.restore(1)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNotNull();
          assertThat(saldo.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── deletePermanent ─── */

  @Test
  @DisplayName("deletePermanent deletes and returns true")
  void deletePermanentTrue(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(1);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deletePermanent(1)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deletePermanent returns false when no rows affected")
  void deletePermanentFalse(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(0);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deletePermanent(99)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isFalse();
          ctx.completeNow();
        })));
  }

  /* ─── restoreAll ─── */

  @Test
  @DisplayName("restoreAll returns count of restored saldos")
  void restoreAllSuccess(VertxTestContext ctx) {
    mockQuery();
    stubDeleteResult(3);

    repo.restoreAll()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(3);
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllPermanent ─── */

  @Test
  @DisplayName("deleteAllPermanent returns count of deleted saldos")
  void deleteAllPermanentSuccess(VertxTestContext ctx) {
    mockQuery();
    stubDeleteResult(2);

    repo.deleteAllPermanent()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(2);
          ctx.completeNow();
        })));
  }

  /* ─── checkCardExists ─── */

  @Test
  @DisplayName("checkCardExists returns true when card exists")
  void checkCardExistsTrue(VertxTestContext ctx) {
    mockPool();
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.next()).thenReturn(row);
    when(row.getLong(0)).thenReturn(1L);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.checkCardExists("4111111111111111")
        .onComplete(ctx.succeeding(exists -> ctx.verify(() -> {
          assertThat(exists).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("checkCardExists returns false when card does not exist")
  void checkCardExistsFalse(VertxTestContext ctx) {
    mockPool();
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.next()).thenReturn(row);
    when(row.getLong(0)).thenReturn(0L);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.checkCardExists("0000000000000000")
        .onComplete(ctx.succeeding(exists -> ctx.verify(() -> {
          assertThat(exists).isFalse();
          ctx.completeNow();
        })));
  }
}
