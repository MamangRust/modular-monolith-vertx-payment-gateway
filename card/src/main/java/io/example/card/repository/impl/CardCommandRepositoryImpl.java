package io.example.card.repository.impl;

import java.util.Random;
import io.example.card.handler.ProtoConverter;
import io.example.card.model.Card;
import io.example.card.repository.CardCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import pb.card.CardCommand.CreateCardRequest;
import pb.card.CardCommand.UpdateCardRequest;

@RequiredArgsConstructor
public class CardCommandRepositoryImpl implements CardCommandRepository {
  private final Pool pool;

  private String generateRandomCardNumber() {
    Random rand = new Random();
    StringBuilder sb = new StringBuilder("4"); // Visa prefix
    for (int i = 0; i < 15; i++) {
      sb.append(rand.nextInt(10));
    }
    return sb.toString();
  }

  @Override
  public Future<Card> createCard(CreateCardRequest request) {
    String cardNumber = generateRandomCardNumber();
    String expireDate = ProtoConverter.formatExpDate(request.getExpireDate());

    return pool
        .preparedQuery(
            """
                INSERT INTO cards (user_id, card_number, card_type, expire_date, cvv, card_provider)
                VALUES ($1, $2, $3, $4, $5, $6)
                RETURNING card_id AS id, user_id, card_number, card_type, expire_date, cvv, card_provider, created_at, updated_at, deleted_at
                """)
        .execute(Tuple.of(
            request.getUserId(),
            cardNumber,
            request.getCardType(),
            expireDate,
            request.getCvv(),
            request.getCardProvider()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Card> updateCard(UpdateCardRequest request) {
    String expireDate = request.hasExpireDate() ? ProtoConverter.formatExpDate(request.getExpireDate()) : null;

    return pool
        .preparedQuery(
            """
                UPDATE cards SET
                    user_id = COALESCE(NULLIF($1, 0), user_id),
                    card_type = COALESCE(NULLIF($2, ''), card_type),
                    expire_date = COALESCE($3, expire_date),
                    cvv = COALESCE(NULLIF($4, ''), cvv),
                    card_provider = COALESCE(NULLIF($5, ''), card_provider),
                    updated_at = NOW()
                WHERE card_id = $6 AND deleted_at IS NULL
                RETURNING card_id AS id, user_id, card_number, card_type, expire_date, cvv, card_provider, created_at, updated_at, deleted_at
                """)
        .execute(Tuple.of(
            request.getUserId(),
            request.getCardType(),
            expireDate,
            request.getCvv(),
            request.getCardProvider(),
            request.getCardId()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Card> trashedCard(Integer cardId) {
    return pool
        .preparedQuery(
            "UPDATE cards SET deleted_at = NOW() WHERE card_id = $1 AND deleted_at IS NULL RETURNING card_id AS id, user_id, card_number, card_type, expire_date, cvv, card_provider, created_at, updated_at, deleted_at")
        .execute(Tuple.of(cardId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Card> restoreCard(Integer cardId) {
    return pool
        .preparedQuery(
            "UPDATE cards SET deleted_at = NULL WHERE card_id = $1 AND deleted_at IS NOT NULL RETURNING card_id AS id, user_id, card_number, card_type, expire_date, cvv, card_provider, created_at, updated_at, deleted_at")
        .execute(Tuple.of(cardId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Boolean> deleteCardPermanent(Integer cardId) {
    return pool
        .preparedQuery("DELETE FROM cards WHERE card_id = $1")
        .execute(Tuple.of(cardId))
        .map(rows -> rows.rowCount() > 0);
  }

  @Override
  public Future<Integer> restoreAllCards() {
    return pool
        .preparedQuery("UPDATE cards SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
        .execute()
        .map(RowSet::rowCount);
  }

  @Override
  public Future<Integer> deleteAllCardsPermanent() {
    return pool
        .preparedQuery("DELETE FROM cards WHERE deleted_at IS NOT NULL")
        .execute()
        .map(RowSet::rowCount);
  }

  @Override
  public Future<Boolean> checkUserExists(Integer userId) {
    return pool
        .preparedQuery("SELECT EXISTS(SELECT 1 FROM users WHERE user_id = $1 AND deleted_at IS NULL)")
        .execute(Tuple.of(userId))
        .map(rows -> rows.iterator().next().getBoolean(0));
  }

  private Card mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Card.fromRow(rows.iterator().next()) : null;
  }
}
