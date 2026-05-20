package io.example.withdraw.repository;

import io.example.common.exception.NotFoundException;
import io.vertx.core.Future;
import pb.card.Card.*;
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

  public Future<CardWithEmailResponse> findUserCardByCardNumber(String cardNumber) {
    return cardStub.findUserCardByCardNumber(FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
        .compose(resp -> {
          if (resp.getEmail() == null || resp.getEmail().isEmpty()) {
            return Future.failedFuture(new NotFoundException("Card user not found"));
          }
          return Future.succeededFuture(resp);
        });
  }
}
