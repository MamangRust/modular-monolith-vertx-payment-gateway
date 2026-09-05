package io.example.saldo.repository.impl;

import io.example.saldo.domain.requests.FindAllSaldos;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class SaldoQueryRepositoryImplTest {

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

  private SaldoQueryRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new SaldoQueryRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
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

  private void mockSaldoRowWithCount() {
    mockSaldoRow();
    when(row.getInteger("total_count")).thenReturn(1);
  }

  private void stubSingleRow() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
  }

  private void stubNoRows() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);
  }

  /* ─── getSaldos ─── */

  @Test
  @DisplayName("getSaldos returns paged result when found")
  void getSaldosFound(VertxTestContext ctx) {
    mockPool();
    mockSaldoRowWithCount();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllSaldos.builder().page(1).pageSize(10).build();
    repo.getSaldos(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          assertThat(result.getData().get(0).getId()).isEqualTo(1);
          assertThat(result.getData().get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getSaldos returns empty result when no saldos")
  void getSaldosEmpty(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllSaldos.builder().page(1).pageSize(10).build();
    repo.getSaldos(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).isEmpty();
          assertThat(result.getTotalRecords()).isZero();
          ctx.completeNow();
        })));
  }

  /* ─── getActiveSaldos ─── */

  @Test
  @DisplayName("getActiveSaldos delegates to getSaldos")
  void getActiveSaldos(VertxTestContext ctx) {
    mockPool();
    mockSaldoRowWithCount();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllSaldos.builder().page(1).pageSize(10).search("").build();
    repo.getActiveSaldos(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedSaldos ─── */

  @Test
  @DisplayName("getTrashedSaldos returns paged result when found")
  void getTrashedSaldosFound(VertxTestContext ctx) {
    mockPool();
    mockSaldoRowWithCount();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllSaldos.builder().page(1).pageSize(10).build();
    repo.getTrashedSaldos(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getSaldoById ─── */

  @Test
  @DisplayName("getSaldoById returns saldo when found")
  void getSaldoByIdFound(VertxTestContext ctx) {
    mockPool();
    mockSaldoRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getSaldoById(1)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNotNull();
          assertThat(saldo.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getSaldoById returns null when not found")
  void getSaldoByIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getSaldoById(99)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── findByTrashedId ─── */

  @Test
  @DisplayName("findByTrashedId returns saldo when found in trashed")
  void findByTrashedIdFound(VertxTestContext ctx) {
    mockPool();
    mockSaldoRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashedId(1)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNotNull();
          assertThat(saldo.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashedId returns null when not found in trashed")
  void findByTrashedIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashedId(99)
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── getSaldoByCardNumber ─── */

  @Test
  @DisplayName("getSaldoByCardNumber returns saldo when found")
  void getSaldoByCardNumberFound(VertxTestContext ctx) {
    mockPool();
    mockSaldoRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getSaldoByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNotNull();
          assertThat(saldo.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getSaldoByCardNumber returns null when not found")
  void getSaldoByCardNumberNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getSaldoByCardNumber("0000000000000000")
        .onComplete(ctx.succeeding(saldo -> ctx.verify(() -> {
          assertThat(saldo).isNull();
          ctx.completeNow();
        })));
  }
}
