package io.example.withdraw.handler;

import io.example.common.model.PaginationMeta;
import io.example.withdraw.service.WithdrawQueryService;
import io.vertx.core.Future;
import pb.withdraw.Withdraw.ApiResponseWithdraw;
import pb.withdraw.Withdraw.ApiResponsesWithdraw;
import pb.withdraw.Withdraw.FindAllWithdrawByCardNumberRequest;
import pb.withdraw.Withdraw.FindAllWithdrawRequest;
import pb.withdraw.Withdraw.FindByIdWithdrawRequest;
import pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdraw;
import pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdrawDeleteAt;

public class WithdrawQueryHandler implements pb.withdraw.VertxWithdrawQueryServiceGrpcServer.WithdrawQueryServiceApi {
  private final WithdrawQueryService service;

  public WithdrawQueryHandler(WithdrawQueryService service) {
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
  public Future<ApiResponsePaginationWithdraw> findAllWithdraw(FindAllWithdrawRequest req) {
    return service.getWithdraws(req)
        .map(resp -> ApiResponsePaginationWithdraw.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromWithdrawResponse).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationWithdraw> findAllWithdrawByCardNumber(FindAllWithdrawByCardNumberRequest req) {
    return service.getWithdrawsByCardNumber(req)
        .map(resp -> ApiResponsePaginationWithdraw.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromWithdrawResponse).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponseWithdraw> findByIdWithdraw(FindByIdWithdrawRequest req) {
    return service.getWithdrawById(req.getWithdrawId())
        .map(resp -> {
          var builder = ApiResponseWithdraw.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromWithdrawResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponsesWithdraw> findByCardNumber(pb.card.Card.FindByCardNumberRequest req) {
    return service.getWithdrawsByCardNumberPrimitive(req.getCardNumber())
        .map(resp -> {
          var builder = ApiResponsesWithdraw.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.addAllData(resp.data().stream().map(ProtoConverter::fromWithdrawResponse).toList());
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponsePaginationWithdrawDeleteAt> findByActive(FindAllWithdrawRequest req) {
    return service.getActiveWithdraws(req)
        .map(resp -> ApiResponsePaginationWithdrawDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromWithdrawResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationWithdrawDeleteAt> findByTrashed(FindAllWithdrawRequest req) {
    return service.getTrashedWithdraws(req)
        .map(resp -> ApiResponsePaginationWithdrawDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromWithdrawResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }
}
