package io.example.card.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.card.service.CardCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.Card.FindByIdCardRequest;
import pb.card.CardCommand.ApiResponseCardAll;
import pb.card.CardCommand.ApiResponseCardDelete;
import pb.card.CardCommand.CreateCardRequest;
import pb.card.CardCommand.UpdateCardRequest;

@RequiredArgsConstructor
public class CardCommandHandler implements pb.card.VertxCardCommandServiceGrpcServer.CardCommandServiceApi {
    private final CardCommandService service;

    @Override
    public Future<pb.card.Card.ApiResponseCard> createCard(CreateCardRequest req) {
        return service.createCard(req)
                .map(data -> pb.card.Card.ApiResponseCard.newBuilder()
                        .setStatus("success")
                        .setMessage("OK")
                        .setData(ProtoConverter.toResponse(data))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<pb.card.Card.ApiResponseCard> updateCard(UpdateCardRequest req) {
        return service.updateCard(req)
                .map(data -> pb.card.Card.ApiResponseCard.newBuilder()
                        .setStatus("success")
                        .setMessage("OK")
                        .setData(ProtoConverter.toResponse(data))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<pb.card.Card.ApiResponseCardDeleteAt> trashedCard(FindByIdCardRequest req) {
        return service.trashedCard(req.getCardId())
                .map(data -> pb.card.Card.ApiResponseCardDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("OK")
                        .setData(ProtoConverter.toResponseDeleted(data))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<pb.card.Card.ApiResponseCardDeleteAt> restoreCard(FindByIdCardRequest req) {
        return service.restoreCard(req.getCardId())
                .map(data -> pb.card.Card.ApiResponseCardDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("OK")
                        .setData(ProtoConverter.toResponseDeleted(data))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseCardDelete> deleteCardPermanent(FindByIdCardRequest req) {
        return service.deleteCardPermanent(req.getCardId())
                .map(v -> ApiResponseCardDelete.newBuilder().setStatus("success").setMessage("OK").build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseCardAll> restoreAllCard(com.google.protobuf.Empty req) {
        return service.restoreAllCard()
                .map(v -> ApiResponseCardAll.newBuilder().setStatus("success").setMessage("OK").build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseCardAll> deleteAllCardPermanent(com.google.protobuf.Empty req) {
        return service.deleteAllCardPermanent()
                .map(v -> ApiResponseCardAll.newBuilder().setStatus("success").setMessage("OK").build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }
}