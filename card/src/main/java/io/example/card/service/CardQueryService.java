package io.example.card.service;

import io.example.card.model.Card;
import io.example.card.model.CardEmail;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;
import pb.card.Card.FindAllCardRequest;

public interface CardQueryService {
  Future<PagedResult<Card>> getCards(FindAllCardRequest request);

  Future<PagedResult<Card>> getActiveCards(FindAllCardRequest request);

  Future<PagedResult<Card>> getTrashedCards(FindAllCardRequest request);

  Future<Card> getCardById(Integer cardId);

  Future<Card> getCardByUserId(Integer userId);

  Future<Card> getCardByCardNumber(String cardNumber);

  Future<CardEmail> getCardEmailByCardNumber(String cardNumber);
}