package io.example.card.repository.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.card.model.Card;
import io.example.card.model.CardEmail;
import io.example.card.repository.CardQueryRepository;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import pb.card.Card.FindAllCardRequest;

@RequiredArgsConstructor
public class CardQueryRepositoryImpl implements CardQueryRepository {
  private final Pool pool;

  private String normalizeSearch(String search) {
    return (search == null || search.isBlank()) ? null : search;
  }

  @Override
  public Future<PagedResult<Card>> findAllCards(FindAllCardRequest req) {
    int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
    String keyword = normalizeSearch(req.getSearch());
    return pool
        .preparedQuery(
            """
                SELECT
                    card_id AS id, user_id, card_number, card_type, expire_date, cvv, card_provider, created_at, updated_at, deleted_at,
                    COUNT(*) OVER() AS total_count
                FROM cards
                WHERE ($1::TEXT IS NULL OR card_number ILIKE '%' || $1 || '%' OR card_type ILIKE '%' || $1 || '%' OR card_provider ILIKE '%' || $1 || '%')
                ORDER BY created_at ASC LIMIT $2 OFFSET $3
                """)
        .execute(Tuple.of(keyword, req.getPageSize(), offset))
        .map(this::mapPagedCards);
  }

  @Override
  public Future<PagedResult<Card>> findByActive(FindAllCardRequest req) {
    int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
    String keyword = normalizeSearch(req.getSearch());
    return pool
        .preparedQuery(
            """
                SELECT
                    card_id AS id, user_id, card_number, card_type, expire_date, cvv, card_provider, created_at, updated_at, deleted_at,
                    COUNT(*) OVER() AS total_count
                FROM cards
                WHERE deleted_at IS NULL
                  AND ($1::TEXT IS NULL OR card_number ILIKE '%' || $1 || '%' OR card_type ILIKE '%' || $1 || '%' OR card_provider ILIKE '%' || $1 || '%')
                ORDER BY created_at ASC LIMIT $2 OFFSET $3
                """)
        .execute(Tuple.of(keyword, req.getPageSize(), offset))
        .map(this::mapPagedCards);
  }

  @Override
  public Future<PagedResult<Card>> findByTrashed(FindAllCardRequest req) {
    int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
    String keyword = normalizeSearch(req.getSearch());
    return pool
        .preparedQuery(
            """
                SELECT
                    card_id AS id, user_id, card_number, card_type, expire_date, cvv, card_provider, created_at, updated_at, deleted_at,
                    COUNT(*) OVER() AS total_count
                FROM cards
                WHERE deleted_at IS NOT NULL
                  AND ($1::TEXT IS NULL OR card_number ILIKE '%' || $1 || '%' OR card_type ILIKE '%' || $1 || '%' OR card_provider ILIKE '%' || $1 || '%')
                ORDER BY deleted_at DESC LIMIT $2 OFFSET $3
                """)
        .execute(Tuple.of(keyword, req.getPageSize(), offset))
        .map(this::mapPagedCards);
  }

  @Override
  public Future<Card> findById(Integer cardId) {
    return pool
        .preparedQuery(
            """
                SELECT card_id AS id, user_id, card_number, card_type, expire_date, cvv, card_provider, created_at, updated_at, deleted_at
                FROM cards WHERE card_id = $1 AND deleted_at IS NULL
                """)
        .execute(Tuple.of(cardId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Card> findByUserId(Integer userId) {
    return pool
        .preparedQuery(
            """
                SELECT card_id AS id, user_id, card_number, card_type, expire_date, cvv, card_provider, created_at, updated_at, deleted_at
                FROM cards WHERE user_id = $1 AND deleted_at IS NULL LIMIT 1
                """)
        .execute(Tuple.of(userId))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Card> findByCardNumber(String cardNumber) {
    return pool
        .preparedQuery(
            """
                SELECT card_id AS id, user_id, card_number, card_type, expire_date, cvv, card_provider, created_at, updated_at, deleted_at
                FROM cards WHERE card_number = $1 AND deleted_at IS NULL
                """)
        .execute(Tuple.of(cardNumber))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<CardEmail> getCardEmailByCardNumber(String cardNumber) {
    return pool
        .preparedQuery(
            """
                SELECT
                  c.card_id AS id, u.email, c.user_id, c.card_number, c.card_type, c.expire_date, c.cvv, c.card_provider, c.created_at, c.updated_at
                FROM cards c JOIN users u ON u.user_id = c.user_id
                WHERE c.card_number = $1 AND c.deleted_at IS NULL AND u.deleted_at IS NULL
                """)
        .execute(Tuple.of(cardNumber))
        .map(rows -> rows.iterator().hasNext() ? CardEmail.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<Card> findByTrashId(Integer cardId) {
    return pool
        .preparedQuery(
            """
                SELECT card_id AS id, user_id, card_number, card_type, expire_date, cvv, card_provider, created_at, updated_at, deleted_at
                FROM cards WHERE card_id = $1 AND deleted_at IS NOT NULL
                """)
        .execute(Tuple.of(cardId))
        .map(this::mapSingleOrNull);
  }

  private Card mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Card.fromRow(rows.iterator().next()) : null;
  }

  private PagedResult<Card> mapPagedCards(RowSet<Row> rows) {
    List<Card> list = new ArrayList<>();
    int total = 0;
    for (Row row : rows) {
      list.add(Card.fromRow(row));
      if (total == 0) {
        total = row.getInteger("total_count");
      }
    }
    return new PagedResult<>(list, total);
  }
}
