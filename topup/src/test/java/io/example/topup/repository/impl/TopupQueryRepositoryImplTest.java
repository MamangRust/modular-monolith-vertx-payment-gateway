package io.example.topup.repository.impl;

import io.example.common.domain.PagedResult;
import io.example.topup.domain.requests.topup.FindAllTopups;
import io.example.topup.domain.requests.topup.FindAllTopupsByCardNumber;
import io.example.topup.model.Topup;
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
class TopupQueryRepositoryImplTest {

  /** topups.topup_no is a UUID column (V12 migration), so the driver decodes it as UUID. */
  private static final UUID TOPUP_NO = UUID.fromString("11111111-2222-3333-4444-555555555555");

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

  private TopupQueryRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TopupQueryRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockTopupRow() {
    when(row.getInteger("topup_id")).thenReturn(1);
    when(row.getString("card_number")).thenReturn("4111111111111111");
    when(row.getUUID("topup_no")).thenReturn(TOPUP_NO);
    when(row.getLong("topup_amount")).thenReturn(500_000L);
    when(row.getString("topup_method")).thenReturn("BANK_TRANSFER");
    when(row.get(LocalDateTime.class, "topup_time")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(null);
  }

  private void mockTopupRowWithCount() {
    mockTopupRow();
    when(row.getValue("total_count")).thenReturn(1);
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

  /* ─── getTopups ─── */

  @Test
  @DisplayName("getTopups returns paged result when found")
  void getTopupsFound(VertxTestContext ctx) {
    mockPool();
    mockTopupRowWithCount();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllTopups.builder().page(1).pageSize(10).build();
    repo.getTopups(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          assertThat(result.getData().get(0).getId()).isEqualTo(1);
          assertThat(result.getData().get(0).getCardNumber()).isEqualTo("4111111111111111");
          assertThat(result.getData().get(0).getTopupNo()).isEqualTo(TOPUP_NO.toString());
          assertThat(result.getData().get(0).getTopupAmount()).isEqualTo(500_000L);
          assertThat(result.getData().get(0).getTopupMethod()).isEqualTo("BANK_TRANSFER");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTopups returns empty result when no topups")
  void getTopupsEmpty(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllTopups.builder().page(1).pageSize(10).build();
    repo.getTopups(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).isEmpty();
          assertThat(result.getTotalRecords()).isZero();
          ctx.completeNow();
        })));
  }

  /* ─── getActiveTopups ─── */

  @Test
  @DisplayName("getActiveTopups returns paged result when found")
  void getActiveTopupsFound(VertxTestContext ctx) {
    mockPool();
    mockTopupRowWithCount();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllTopups.builder().page(1).pageSize(10).build();
    repo.getActiveTopups(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          assertThat(result.getData().get(0).getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getTopupsByCardNumber ─── */

  @Test
  @DisplayName("getTopupsByCardNumber returns paged result when found")
  void getTopupsByCardNumberFound(VertxTestContext ctx) {
    mockPool();
    mockTopupRowWithCount();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllTopupsByCardNumber.builder().cardNumber("4111111111111111").page(1).pageSize(10).build();
    repo.getTopupsByCardNumber(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          assertThat(result.getData().get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedTopups ─── */

  @Test
  @DisplayName("getTrashedTopups returns paged result when found")
  void getTrashedTopupsFound(VertxTestContext ctx) {
    mockPool();
    mockTopupRowWithCount();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllTopups.builder().page(1).pageSize(10).build();
    repo.getTrashedTopups(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          assertThat(result.getData().get(0).getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getTopupById ─── */

  @Test
  @DisplayName("getTopupById returns topup when found")
  void getTopupByIdFound(VertxTestContext ctx) {
    mockPool();
    mockTopupRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getTopupById(1)
        .onComplete(ctx.succeeding(topup -> ctx.verify(() -> {
          assertThat(topup).isNotNull();
          assertThat(topup.getId()).isEqualTo(1);
          assertThat(topup.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTopupById returns null when not found")
  void getTopupByIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getTopupById(99)
        .onComplete(ctx.succeeding(topup -> ctx.verify(() -> {
          assertThat(topup).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── getTopupByCardNumber ─── */

  @Test
  @DisplayName("getTopupByCardNumber returns topup when found")
  void getTopupByCardNumberFound(VertxTestContext ctx) {
    mockPool();
    mockTopupRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getTopupByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(topup -> ctx.verify(() -> {
          assertThat(topup).isNotNull();
          assertThat(topup.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTopupByCardNumber returns null when not found")
  void getTopupByCardNumberNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getTopupByCardNumber("0000000000000000")
        .onComplete(ctx.succeeding(topup -> ctx.verify(() -> {
          assertThat(topup).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── findByTrashed ─── */

  @Test
  @DisplayName("findByTrashed returns topup when found in trashed")
  void findByTrashedFound(VertxTestContext ctx) {
    mockPool();
    mockTopupRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashed(1)
        .onComplete(ctx.succeeding(topup -> ctx.verify(() -> {
          assertThat(topup).isNotNull();
          assertThat(topup.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashed returns null when not found in trashed")
  void findByTrashedNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashed(99)
        .onComplete(ctx.succeeding(topup -> ctx.verify(() -> {
          assertThat(topup).isNull();
          ctx.completeNow();
        })));
  }
}
