package io.example.topup.repository;

import io.example.common.exception.NotFoundException;
import io.vertx.core.Future;
import pb.card.Card.ApiResponseCard;
import pb.card.Card.CardWithEmailResponse;
import pb.card.Card.FindByCardNumberRequest;
import pb.card.CardCommand.UpdateCardRequest;
import pb.card.VertxCardCommandServiceGrpcClient;
import pb.card.VertxCardQueryServiceGrpcClient;

public class CardClientRepository {
  private final VertxCardQueryServiceGrpcClient queryStub;
  private final VertxCardCommandServiceGrpcClient commandStub;

  public CardClientRepository(VertxCardQueryServiceGrpcClient queryStub,
      VertxCardCommandServiceGrpcClient commandStub) {
    this.queryStub = queryStub;
    this.commandStub = commandStub;
  }

  public Future<ApiResponseCard> getCardByCardNumber(String cardNumber) {
    return queryStub.findByCardNumber(FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
        .compose(resp -> {
          if (resp.getStatus().equals("error")) {
            return Future.failedFuture(new NotFoundException(resp.getMessage()));
          }
          return Future.succeededFuture(resp);
        });
  }

  public Future<CardWithEmailResponse> getCardEmailByCardNumber(String cardNumber) {
    return queryStub.findUserCardByCardNumber(FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
        .compose(resp -> {
          // Check if successful based on response fields if status is not explicitly
          // present in CardWithEmailResponse
          // Usually these responses have some indicator.
          return Future.succeededFuture(resp);
        });
  }

  public Future<ApiResponseCard> updateCard(UpdateCardRequest req) {
    return commandStub.updateCard(req)
        .compose(resp -> {
          if (resp.getStatus().equals("error")) {
            return Future.failedFuture(new RuntimeException(resp.getMessage()));
          }
          return Future.succeededFuture(resp);
        });
  }
}
