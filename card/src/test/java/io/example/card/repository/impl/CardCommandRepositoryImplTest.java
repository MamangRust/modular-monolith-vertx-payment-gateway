package io.example.card.repository.impl;

import io.example.card.model.Card;
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

import pb.card.CardCommand.CreateCardRequest;
import pb.card.CardCommand.UpdateCardRequest;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardCommandRepositoryImplTest {

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

  private CardCommandRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new CardCommandRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private Timestamp now() {
    return Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
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

  private void stubDeleteResult(int count) {
    when(rowSet.rowCount()).thenReturn(count);
  }

  /* ─── createCard ─── */

  @Test
  @DisplayName("createCard inserts and returns new card")
  void createCardSuccess(VertxTestContext ctx) {
    mockPool();
    mockCardRow();
    // mapSingleOrNull checks hasNext() then calls next()
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = CreateCardRequest.newBuilder()
        .setUserId(42)
        .setCardType("CREDIT")
        .setCvv("123")
        .setCardProvider("VISA")
        .build();

    repo.createCard(req)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNotNull();
          assertThat(card.getId()).isEqualTo(1);
          assertThat(card.getUserId()).isEqualTo(42);
          assertThat(card.getCardNumber()).isNotNull(); // auto-generated
          ctx.completeNow();
        })));
  }

  /* ─── updateCard ─── */

  @Test
  @DisplayName("updateCard updates and returns card")
  void updateCardSuccess(VertxTestContext ctx) {
    mockPool();
    mockCardRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateCardRequest.newBuilder()
        .setCardId(1)
        .setUserId(42)
        .setCardType("DEBIT")
        .build();

    repo.updateCard(req)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNotNull();
          assertThat(card.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateCard returns null when card not found")
  void updateCardNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateCardRequest.newBuilder().setCardId(99).setUserId(1).build();

    repo.updateCard(req)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── trashedCard ─── */

  @Test
  @DisplayName("trashedCard soft-deletes and returns card")
  void trashedCardSuccess(VertxTestContext ctx) {
    mockPool();
    mockCardRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashedCard(1)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNotNull();
          assertThat(card.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashedCard returns null when card not found")
  void trashedCardNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashedCard(99)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── restoreCard ─── */

  @Test
  @DisplayName("restoreCard restores and returns card")
  void restoreCardSuccess(VertxTestContext ctx) {
    mockPool();
    mockCardRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreCard(1)
        .onComplete(ctx.succeeding(card -> ctx.verify(() -> {
          assertThat(card).isNotNull();
          assertThat(card.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── deleteCardPermanent ─── */

  @Test
  @DisplayName("deleteCardPermanent deletes and returns true")
  void deleteCardPermanentTrue(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(1);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteCardPermanent(1)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteCardPermanent returns false when no rows affected")
  void deleteCardPermanentFalse(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(0);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteCardPermanent(99)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isFalse();
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllCards ─── */

  @Test
  @DisplayName("restoreAllCards returns count of restored cards")
  void restoreAllCardsSuccess(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(3);

    when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreAllCards()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(3);
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllCardsPermanent ─── */

  @Test
  @DisplayName("deleteAllCardsPermanent returns count of deleted cards")
  void deleteAllCardsPermanentSuccess(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(2);

    when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteAllCardsPermanent()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(2);
          ctx.completeNow();
        })));
  }

  /* ─── checkUserExists ─── */

  @Test
  @DisplayName("checkUserExists returns true when user exists")
  void checkUserExistsTrue(VertxTestContext ctx) {
    mockPool();
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.next()).thenReturn(row);
    when(row.getBoolean(0)).thenReturn(true);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.checkUserExists(42)
        .onComplete(ctx.succeeding(exists -> ctx.verify(() -> {
          assertThat(exists).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("checkUserExists returns false when user does not exist")
  void checkUserExistsFalse(VertxTestContext ctx) {
    mockPool();
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.next()).thenReturn(row);
    when(row.getBoolean(0)).thenReturn(false);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.checkUserExists(99)
        .onComplete(ctx.succeeding(exists -> ctx.verify(() -> {
          assertThat(exists).isFalse();
          ctx.completeNow();
        })));
  }
}
