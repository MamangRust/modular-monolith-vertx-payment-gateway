package io.example.transaction.repository;

import io.example.common.exception.NotFoundException;
import io.vertx.core.Future;
import pb.card.Card.ApiResponseCard;
import pb.card.Card.CardWithEmailResponse;
import pb.card.Card.FindByCardNumberRequest;
import pb.card.Card.FindByUserIdCardRequest;
import pb.card.VertxCardQueryServiceGrpcClient;

public class CardClientRepository {
  private final VertxCardQueryServiceGrpcClient cardStub;

  public CardClientRepository(VertxCardQueryServiceGrpcClient cardStub) {
    this.cardStub = cardStub;
  }

  public Future<ApiResponseCard> getCardByCardNumber(String cardNumber) {
    return cardStub.findByCardNumber(FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
        .compose(resp -> {
          if (resp.getStatus().equals("error")) {
            return Future.failedFuture(new NotFoundException(resp.getMessage()));
          }
          return Future.succeededFuture(resp);
        });
  }

  public Future<ApiResponseCard> getCardByUserId(int userId) {
    return cardStub.findByUserIdCard(FindByUserIdCardRequest.newBuilder().setUserId(userId).build())
        .compose(resp -> {
          if (resp.getStatus().equals("error")) {
            return Future.failedFuture(new NotFoundException(resp.getMessage()));
          }
          return Future.succeededFuture(resp);
        });
  }

  public Future<CardWithEmailResponse> getUserCardByCardNumber(String cardNumber) {
    return cardStub.findUserCardByCardNumber(FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build());
  }
}
