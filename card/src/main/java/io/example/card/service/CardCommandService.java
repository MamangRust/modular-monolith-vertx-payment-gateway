package io.example.card.service;

import io.example.card.model.Card;
import io.example.common.domain.ApiResponse;
import io.vertx.core.Future;
import pb.card.CardCommand.CreateCardRequest;
import pb.card.CardCommand.UpdateCardRequest;

public interface CardCommandService {
  Future<ApiResponse<Card>> createCard(CreateCardRequest request);
  Future<ApiResponse<Card>> updateCard(UpdateCardRequest request);
  Future<ApiResponse<Card>> trashedCard(Integer cardId);
  Future<ApiResponse<Card>> restoreCard(Integer cardId);
  Future<ApiResponse<Boolean>> deleteCardPermanent(Integer cardId);
  Future<ApiResponse<Boolean>> restoreAllCard();
  Future<ApiResponse<Boolean>> deleteAllCardPermanent();
}
