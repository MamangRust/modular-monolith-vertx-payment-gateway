package io.example.card.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.card.service.CardQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.Card.FindAllCardRequest;
import pb.card.Card.FindByCardNumberRequest;
import pb.card.Card.FindByIdCardRequest;
import pb.card.Card.FindByUserIdCardRequest;

@RequiredArgsConstructor
public class CardQueryHandler implements pb.card.VertxCardQueryServiceGrpcServer.CardQueryServiceApi {
  private final CardQueryService service;

  private pb.common.PaginationMeta buildPaginationMeta(int page, int pageSize, int totalRecords) {
    int safePage = page > 0 ? page : 1;
    int safePageSize = pageSize > 0 ? pageSize : 10;
    int totalPages = (int) Math.ceil((double) totalRecords / safePageSize);
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(safePage)
        .setPageSize(safePageSize)
        .setTotalPages(totalPages)
        .setTotalRecords(totalRecords)
        .build();
  }

  @Override
  public Future<pb.card.CardQuery.ApiResponsePaginationCard> findAllCard(FindAllCardRequest req) {
    return service.getCards(req)
        .map(resp -> {
          var builder = pb.card.CardQuery.ApiResponsePaginationCard.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::toResponse).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<pb.card.Card.ApiResponseCard> findByIdCard(FindByIdCardRequest req) {
    return service.getCardById(req.getCardId())
        .map(data -> pb.card.Card.ApiResponseCard.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.toResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<pb.card.Card.ApiResponseCard> findByUserIdCard(FindByUserIdCardRequest req) {
    return service.getCardByUserId(req.getUserId())
        .map(data -> pb.card.Card.ApiResponseCard.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.toResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<pb.card.CardQuery.ApiResponsePaginationCardDeleteAt> findByActiveCard(FindAllCardRequest req) {
    return service.getActiveCards(req)
        .map(resp -> {
          var builder = pb.card.CardQuery.ApiResponsePaginationCardDeleteAt.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::toResponseDeleted).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<pb.card.CardQuery.ApiResponsePaginationCardDeleteAt> findByTrashedCard(FindAllCardRequest req) {
    return service.getTrashedCards(req)
        .map(resp -> {
          var builder = pb.card.CardQuery.ApiResponsePaginationCardDeleteAt.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::toResponseDeleted).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<pb.card.Card.ApiResponseCard> findByCardNumber(FindByCardNumberRequest req) {
    return service.getCardByCardNumber(req.getCardNumber())
        .map(data -> pb.card.Card.ApiResponseCard.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.toResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<pb.card.Card.CardWithEmailResponse> findUserCardByCardNumber(FindByCardNumberRequest req) {
    return service.getCardEmailByCardNumber(req.getCardNumber())
        .map(ProtoConverter::toEmailResponse)
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}