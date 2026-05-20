package io.example.card.repository;

import io.example.card.model.Card;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;
import pb.card.Card.FindAllCardRequest;;

public interface CardQueryRepository {
  Future<PagedResult<Card>> findAllCards(FindAllCardRequest request);

  Future<PagedResult<Card>> findByActive(FindAllCardRequest request);

  Future<PagedResult<Card>> findByTrashed(FindAllCardRequest request);

  Future<Card> findById(Integer cardId);

  Future<Card> findByUserId(Integer userId);

  Future<Card> findByCardNumber(String cardNumber);
}
