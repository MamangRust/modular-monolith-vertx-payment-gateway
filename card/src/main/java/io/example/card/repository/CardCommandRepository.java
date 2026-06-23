package io.example.card.repository;

import io.example.card.model.Card;
import io.vertx.core.Future;
import pb.card.CardCommand.CreateCardRequest;
import pb.card.CardCommand.UpdateCardRequest;

public interface CardCommandRepository {
  Future<Card> createCard(CreateCardRequest request);

  Future<Card> updateCard(UpdateCardRequest request);

  Future<Card> trashedCard(Integer cardId);

  Future<Card> restoreCard(Integer cardId);

  Future<Boolean> deleteCardPermanent(Integer cardId);

  Future<Integer> restoreAllCards();

  Future<Integer> deleteAllCardsPermanent();

  Future<Boolean> checkUserExists(Integer userId);
}
