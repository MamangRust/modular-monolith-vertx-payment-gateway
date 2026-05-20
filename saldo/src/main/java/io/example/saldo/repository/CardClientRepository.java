package io.example.saldo.repository;

import io.example.common.exception.NotFoundException;
import io.vertx.core.Future;
import pb.card.Card.ApiResponseCard;
import pb.card.VertxCardQueryServiceGrpcClient;

public class CardClientRepository {
  private final VertxCardQueryServiceGrpcClient cardStub;

  public CardClientRepository(VertxCardQueryServiceGrpcClient cardStub) {
    this.cardStub = cardStub;
  }

  public Future<ApiResponseCard> getCardByCardNumber(String cardNumber) {
    // FindByCardNumber seems appropriate if it exists, otherwise we use what's
    // available
    // Let's assume FindByCardNumber exists based on monolithic
    // repoCard.getCardByCardNumber
    // If not, I'll check the proto.
    return cardStub
        .findByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
        .compose(resp -> {
          if (resp.getStatus().equals("error")) {
            return Future.failedFuture(new NotFoundException(resp.getMessage()));
          }
          return Future.succeededFuture(resp);
        });
  }
}
