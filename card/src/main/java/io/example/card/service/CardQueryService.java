package io.example.card.service;

import java.util.List;
import io.example.card.model.Card;
import io.example.card.model.CardEmail;
import io.example.common.domain.ApiResponse;
import io.example.common.domain.ApiResponsePagination;
import io.vertx.core.Future;
import pb.card.Card.FindAllCardRequest;
import pb.card.Card.FindByIdCardRequest;
import pb.card.Card.FindByUserIdCardRequest;
import pb.card.Card.FindByCardNumberRequest;

public interface CardQueryService {
  Future<ApiResponsePagination<List<Card>>> getCards(FindAllCardRequest request);
  Future<ApiResponsePagination<List<Card>>> getActiveCards(FindAllCardRequest request);
  Future<ApiResponsePagination<List<Card>>> getTrashedCards(FindAllCardRequest request);
  Future<ApiResponse<Card>> getCardById(FindByIdCardRequest request);
  Future<ApiResponse<Card>> getCardByUserId(FindByUserIdCardRequest request);
  Future<ApiResponse<Card>> getCardByCardNumber(FindByCardNumberRequest request);
  Future<CardEmail> getCardEmailByCardNumber(String cardNumber);
}
