package io.example.topup.handler;

import io.example.common.model.PaginationMeta;
import io.example.topup.service.TopupQueryService;
import io.vertx.core.Future;
import pb.topup.Topup.*;
import pb.topup.TopupQuery.*;

public class TopupQueryHandler implements pb.topup.VertxTopupQueryServiceGrpcServer.TopupQueryServiceApi {
  private final TopupQueryService service;

  public TopupQueryHandler(TopupQueryService service) {
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
  public Future<ApiResponsePaginationTopup> findAllTopup(FindAllTopupRequest req) {
    return service.getTopups(req)
        .map(resp -> ApiResponsePaginationTopup.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromTopupResponse).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationTopup> findAllTopupByCardNumber(FindAllTopupByCardNumberRequest req) {
    // FindAllTopupByCardNumber is not explicitly in TopupQueryService, I'll add it
    // or map it.
    // For now I'll use getTopups with search = cardNumber.
    FindAllTopupRequest queryReq = FindAllTopupRequest.newBuilder()
        .setPage(req.getPage())
        .setPageSize(req.getPageSize())
        .setSearch(req.getCardNumber())
        .build();
    return service.getTopups(queryReq)
        .map(resp -> ApiResponsePaginationTopup.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromTopupResponse).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponseTopup> findByIdTopup(FindByIdTopupRequest req) {
    return service.getTopupById(req.getTopupId())
        .map(resp -> {
          var builder = ApiResponseTopup.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTopupResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTopup> findByCardNumberTopup(FindByCardNumberTopupRequest req) {
    // Use getTopups with limit 1
    FindAllTopupRequest queryReq = FindAllTopupRequest.newBuilder()
        .setPage(1)
        .setPageSize(1)
        .setSearch(req.getCardNumber())
        .build();
    return service.getTopups(queryReq)
        .map(resp -> {
          var builder = ApiResponseTopup.newBuilder()
              .setStatus(resp.status());
          if (resp.data().isEmpty()) {
            builder.setMessage("Topup not found for card");
          } else {
            builder.setMessage(resp.message());
            builder.setData(ProtoConverter.fromTopupResponse(resp.data().get(0)));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponsePaginationTopupDeleteAt> findByActive(FindAllTopupRequest req) {
    return service.getActiveTopups(req)
        .map(resp -> ApiResponsePaginationTopupDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromTopupResponseToProtoDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationTopupDeleteAt> findByTrashed(FindAllTopupRequest req) {
    return service.getTrashedTopups(req)
        .map(resp -> ApiResponsePaginationTopupDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromTopupResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }
}
