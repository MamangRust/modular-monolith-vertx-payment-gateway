package io.example.topup.repository.impl;

import io.example.topup.domain.requests.topup.CreateTopupRequest;
import io.example.topup.domain.requests.topup.UpdateTopupRequest;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TopupCommandRepositoryImplTest {

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

  private TopupCommandRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TopupCommandRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockRow() {
    when(row.getInteger("topup_id")).thenReturn(1);
    when(row.getString("card_number")).thenReturn("4111");
    when(row.getUUID("topup_no")).thenReturn(UUID.fromString("11111111-2222-3333-4444-555555555555"));
    when(row.getLong("topup_amount")).thenReturn(50000L);
    when(row.getString("topup_method")).thenReturn("BANK");
    when(row.get(LocalDateTime.class, "topup_time")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
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

  /* ─── create ─── */

  @Test
  @DisplayName("createTopup returns created topup")
  void create(VertxTestContext ctx) {
    mockPool();
    mockRow();
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.createTopup(CreateTopupRequest.builder().cardNumber("4111").topupAmount(50000).topupMethod("BANK").build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> {
          assertThat(r).isNotNull();
          ctx.completeNow();
        })));
  }

  /* ─── update ─── */

  @Test
  @DisplayName("updateTopup returns updated topup")
  void update(VertxTestContext ctx) {
    mockPool();
    mockRow();
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.updateTopup(UpdateTopupRequest.builder().topupId(1).cardNumber("4111").build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> {
          assertThat(r).isNotNull();
          ctx.completeNow();
        })));
  }

  /* ─── trash ─── */

  @Test
  @DisplayName("trashTopup returns topup when found")
  void trash_found(VertxTestContext ctx) {
    mockPool();
    mockRow();
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashTopup(1)
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> {
          assertThat(r).isNotNull();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashTopup returns null when not found")
  void trash_notFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashTopup(99)
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> {
          assertThat(r).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── restore ─── */

  @Test
  @DisplayName("restoreTopup returns restored topup")
  void restore(VertxTestContext ctx) {
    mockPool();
    mockRow();
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreTopup(1)
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> {
          assertThat(r).isNotNull();
          ctx.completeNow();
        })));
  }

  /* ─── delete permanent ─── */

  @Test
  @DisplayName("deleteTopupPermanently returns true for existing id")
  void deletePermanent_true(VertxTestContext ctx) {
    mockPool();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteTopupPermanently(1)
        .onComplete(ctx.succeeding(b -> ctx.verify(() -> {
          assertThat(b).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteTopupPermanently returns true for non-existing id")
  void deletePermanent_alwaysTrue(VertxTestContext ctx) {
    mockPool();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteTopupPermanently(99)
        .onComplete(ctx.succeeding(b -> ctx.verify(() -> {
          assertThat(b).isTrue();
          ctx.completeNow();
        })));
  }

  /* ─── restore all ─── */

  @Test
  @DisplayName("restoreAllTopups returns count of restored rows")
  void restoreAll(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(query);
    when(query.execute()).thenReturn(Future.succeededFuture(rowSet));
    stubDeleteResult(3);

    repo.restoreAllTopups()
        .onComplete(ctx.succeeding(c -> ctx.verify(() -> {
          assertThat(c).isEqualTo(3);
          ctx.completeNow();
        })));
  }

  /* ─── delete all permanent ─── */

  @Test
  @DisplayName("deleteAllPermanentTopups returns count of deleted rows")
  void deleteAllPermanent(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(query);
    when(query.execute()).thenReturn(Future.succeededFuture(rowSet));
    stubDeleteResult(2);

    repo.deleteAllPermanentTopups()
        .onComplete(ctx.succeeding(c -> ctx.verify(() -> {
          assertThat(c).isEqualTo(2);
          ctx.completeNow();
        })));
  }
}
