package io.example.saldo.repository;

import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.Card.ApiResponseCard;
import pb.card.Card.CardWithEmailResponse;
import pb.card.Card.FindByCardNumberRequest;
import pb.card.VertxCardQueryServiceGrpcClient;
import io.example.common.exception.grpc.NotFoundException;

@RequiredArgsConstructor
public class CardClientRepository {
  private final VertxCardQueryServiceGrpcClient cardStub;

  public Future<ApiResponseCard> getCardByCardNumber(String cardNumber) {
    return cardStub
        .findByCardNumber(FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
        .compose(resp -> {
          if (resp.getStatus().equals("error")) {
            return Future.failedFuture(new NotFoundException(resp.getMessage()));
          }
          return Future.succeededFuture(resp);
        });
  }

  public Future<CardWithEmailResponse> findUserCardByCardNumber(String cardNumber) {
    return cardStub
        .findUserCardByCardNumber(FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
        .compose(resp -> {
          if (resp.getEmail() == null || resp.getEmail().isEmpty()) {
            return Future.failedFuture(new NotFoundException("Card user not found"));
          }
          return Future.succeededFuture(resp);
        });
  }
}
