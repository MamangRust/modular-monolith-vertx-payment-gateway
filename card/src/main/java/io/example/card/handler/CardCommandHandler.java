package io.example.card.handler;

import io.example.card.service.CardCommandService;
import io.vertx.core.Future;
import pb.card.Card.ApiResponseCard;
import pb.card.Card.ApiResponseCardDeleteAt;
import pb.card.Card.FindByIdCardRequest;
import pb.card.CardCommand.ApiResponseCardAll;
import pb.card.CardCommand.ApiResponseCardDelete;
import pb.card.CardCommand.CreateCardRequest;
import pb.card.CardCommand.UpdateCardRequest;

public class CardCommandHandler implements pb.card.VertxCardCommandServiceGrpcServer.CardCommandServiceApi {
  private final CardCommandService service;

  public CardCommandHandler(CardCommandService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseCard> createCard(CreateCardRequest req) {
    return service.createCard(req)
        .map(resp -> ApiResponseCard.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .setData(resp.data() != null ? ProtoConverter.toResponse(resp.data())
                : pb.card.Card.CardResponse.getDefaultInstance())
            .build());
  }

  @Override
  public Future<ApiResponseCard> updateCard(UpdateCardRequest req) {
    return service.updateCard(req)
        .map(resp -> ApiResponseCard.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .setData(resp.data() != null ? ProtoConverter.toResponse(resp.data())
                : pb.card.Card.CardResponse.getDefaultInstance())
            .build());
  }

  @Override
  public Future<ApiResponseCardDeleteAt> trashedCard(FindByIdCardRequest req) {
    return service.trashedCard(req.getCardId())
        .map(resp -> ApiResponseCardDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .setData(resp.data() != null ? ProtoConverter.toResponseDeleted(resp.data())
                : pb.card.Card.CardResponseDeleteAt.getDefaultInstance())
            .build());
  }

  @Override
  public Future<ApiResponseCardDeleteAt> restoreCard(FindByIdCardRequest req) {
    return service.restoreCard(req.getCardId())
        .map(resp -> ApiResponseCardDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .setData(resp.data() != null ? ProtoConverter.toResponseDeleted(resp.data())
                : pb.card.Card.CardResponseDeleteAt.getDefaultInstance())
            .build());
  }

  @Override
  public Future<ApiResponseCardDelete> deleteCardPermanent(FindByIdCardRequest req) {
    return service.deleteCardPermanent(req.getCardId())
        .map(resp -> ApiResponseCardDelete.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseCardAll> restoreAllCard(com.google.protobuf.Empty req) {
    return service.restoreAllCard()
        .map(resp -> ApiResponseCardAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseCardAll> deleteAllCardPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllCardPermanent()
        .map(resp -> ApiResponseCardAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }
}
