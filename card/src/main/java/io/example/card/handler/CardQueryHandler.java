package io.example.card.handler;

import io.example.card.service.CardQueryService;
import io.example.common.domain.PaginationMeta;
import io.vertx.core.Future;
import pb.card.Card.ApiResponseCard;
import pb.card.Card.CardWithEmailResponse;
import pb.card.Card.FindAllCardRequest;
import pb.card.Card.FindByCardNumberRequest;
import pb.card.Card.FindByIdCardRequest;
import pb.card.Card.FindByUserIdCardRequest;
import pb.card.CardQuery.ApiResponsePaginationCard;
import pb.card.CardQuery.ApiResponsePaginationCardDeleteAt;

public class CardQueryHandler implements pb.card.VertxCardQueryServiceGrpcServer.CardQueryServiceApi {
  private final CardQueryService service;

  public CardQueryHandler(CardQueryService service) {
    this.service = service;
  }

  private pb.common.PaginationMeta toMeta(PaginationMeta meta) {
    if (meta == null)
      return pb.common.PaginationMeta.getDefaultInstance();
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(meta.currentPage())
        .setPageSize(meta.pageSize())
        .setTotalPages(meta.totalPages())
        .setTotalRecords(meta.totalRecords())
        .build();
  }

  @Override
  public Future<ApiResponsePaginationCard> findAllCard(FindAllCardRequest req) {
    return service.getCards(req)
        .map(res -> ApiResponsePaginationCard.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(ProtoConverter::toResponse).toList())
            .setPaginationMeta(toMeta(res.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponseCard> findByIdCard(FindByIdCardRequest req) {
    return service.getCardById(req)
        .map(resp -> {
          var builder = ApiResponseCard.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.toResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseCard> findByUserIdCard(FindByUserIdCardRequest req) {
    return service.getCardByUserId(req)
        .map(resp -> {
          var builder = ApiResponseCard.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.toResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponsePaginationCardDeleteAt> findByActiveCard(FindAllCardRequest req) {
    return service.getActiveCards(req)
        .map(res -> ApiResponsePaginationCardDeleteAt.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(ProtoConverter::toResponseDeleted).toList())
            .setPaginationMeta(toMeta(res.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationCardDeleteAt> findByTrashedCard(FindAllCardRequest req) {
    return service.getTrashedCards(req)
        .map(res -> ApiResponsePaginationCardDeleteAt.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(ProtoConverter::toResponseDeleted).toList())
            .setPaginationMeta(toMeta(res.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponseCard> findByCardNumber(FindByCardNumberRequest req) {
    return service.getCardByCardNumber(req)
        .map(resp -> {
          var builder = ApiResponseCard.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.toResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<CardWithEmailResponse> findUserCardByCardNumber(FindByCardNumberRequest req) {
    return service.getCardEmailByCardNumber(req.getCardNumber())
        .map(record -> {
          if (record == null)
            return CardWithEmailResponse.getDefaultInstance();
          return ProtoConverter.toEmailResponse(record);
        });
  }
}
