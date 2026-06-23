package io.example.card.service;

import io.example.card.model.Card;
import io.vertx.core.Future;
import pb.card.CardCommand.CreateCardRequest;
import pb.card.CardCommand.UpdateCardRequest;

public interface CardCommandService {
  Future<Card> createCard(CreateCardRequest request);

  Future<Card> updateCard(UpdateCardRequest request);

  Future<Card> trashedCard(Integer cardId);

  Future<Card> restoreCard(Integer cardId);

  Future<Void> deleteCardPermanent(Integer cardId);

  Future<Void> restoreAllCard();

  Future<Void> deleteAllCardPermanent();
}