package io.example.card.repository.impl;

import io.example.card.model.Card;
import io.example.card.model.CardEmail;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardQueryRepositoryImplTest {

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

  private CardQueryRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new CardQueryRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockCardRow() {
    when(row.getInteger("id")).thenReturn(1);
    when(row.getInteger("user_id")).thenReturn(42);
    when(row.getString("card_number")).thenReturn("4111111111111111");
    when(row.getString("card_type")).thenReturn("CREDIT");
    when(row.getString("expire_date")).thenReturn("2028-12-31");
    when(row.getString("cvv")).thenReturn("123");
    when(row.getString("card_provider")).thenReturn("VISA");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(null);
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

  /* ─── findById ─── */

  @Test
  @DisplayName("findById returns card when found")
  void findByIdFound(VertxTestContext ctx) {
    mockPool();
    mockCardRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findById(1)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNotNull();
          assertThat(card.getId()).isEqualTo(1);
          assertThat(card.getUserId()).isEqualTo(42);
          assertThat(card.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findById returns null when not found")
  void findByIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findById(99)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── findByUserId ─── */

  @Test
  @DisplayName("findByUserId returns card when found")
  void findByUserIdFound(VertxTestContext ctx) {
    mockPool();
    mockCardRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByUserId(42)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNotNull();
          assertThat(card.getUserId()).isEqualTo(42);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByUserId returns null when not found")
  void findByUserIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByUserId(99)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── findByCardNumber ─── */

  @Test
  @DisplayName("findByCardNumber returns card when found")
  void findByCardNumberFound(VertxTestContext ctx) {
    mockPool();
    mockCardRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNotNull();
          assertThat(card.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByCardNumber returns null when not found")
  void findByCardNumberNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByCardNumber("0000000000000000")
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── findByTrashId ─── */

  @Test
  @DisplayName("findByTrashId returns trashed card when found")
  void findByTrashIdFound(VertxTestContext ctx) {
    mockPool();
    mockCardRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashId(1)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNotNull();
          assertThat(card.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashId returns null when not found")
  void findByTrashIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashId(99)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── getCardEmailByCardNumber ─── */

  private void mockCardEmailRow() {
    when(row.getInteger("id")).thenReturn(1);
    when(row.getString("email")).thenReturn("alice@example.com");
    when(row.getInteger("user_id")).thenReturn(42);
    when(row.getString("card_number")).thenReturn("4111111111111111");
    when(row.getString("card_type")).thenReturn("CREDIT");
    when(row.getString("expire_date")).thenReturn("2028-12-31");
    when(row.getString("cvv")).thenReturn("123");
    when(row.getString("card_provider")).thenReturn("VISA");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(rowSet.iterator()).thenReturn(iterator);
  }

  @Test
  @DisplayName("getCardEmailByCardNumber returns card with email when found")
  void getCardEmailByCardNumberFound(VertxTestContext ctx) {
    mockPool();
    mockCardEmailRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getCardEmailByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(ce -> ctx.verify(() -> {
          assertThat(ce).isNotNull();
          assertThat(ce.getEmail()).isEqualTo("alice@example.com");
          assertThat(ce.getCardNumber()).isEqualTo("4111111111111111");
          assertThat(ce.getUserId()).isEqualTo(42);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getCardEmailByCardNumber returns null when not found")
  void getCardEmailByCardNumberNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getCardEmailByCardNumber("0000000000000000")
        .onComplete(ctx.succeeding(ce -> ctx.verify(() -> {
          assertThat(ce).isNull();
          ctx.completeNow();
        })));
  }
}
