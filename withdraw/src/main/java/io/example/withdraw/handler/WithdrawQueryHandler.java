package io.example.withdraw.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.withdraw.domain.requests.FindAllWithdraws;
import io.example.withdraw.domain.requests.FindAllWithdrawCardNumber;
import io.example.withdraw.service.WithdrawQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.withdraw.Withdraw.ApiResponseWithdraw;
import pb.withdraw.Withdraw.ApiResponsesWithdraw;
import pb.withdraw.Withdraw.FindAllWithdrawByCardNumberRequest;
import pb.withdraw.Withdraw.FindAllWithdrawRequest;
import pb.withdraw.Withdraw.FindByIdWithdrawRequest;
import pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdraw;
import pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdrawDeleteAt;

@RequiredArgsConstructor
public class WithdrawQueryHandler implements pb.withdraw.VertxWithdrawQueryServiceGrpcServer.WithdrawQueryServiceApi {
  private final WithdrawQueryService service;

  private pb.common.PaginationMeta toMeta(int totalRecords, int page, int pageSize) {
    int currentPage = page > 0 ? page : 1;
    int size = pageSize > 0 ? pageSize : 10;
    int totalPages = size > 0 ? (int) Math.ceil((double) totalRecords / size) : 0;
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(currentPage)
        .setPageSize(size)
        .setTotalPages(totalPages)
        .setTotalRecords(totalRecords)
        .build();
  }

  private FindAllWithdraws toDomainReq(FindAllWithdrawRequest req) {
    return FindAllWithdraws.builder()
        .search(req.getSearch())
        .page(req.getPage() > 0 ? req.getPage() : 1)
        .pageSize(req.getPageSize() > 0 ? req.getPageSize() : 10)
        .build();
  }

  @Override
  public Future<ApiResponsePaginationWithdraw> findAllWithdraw(FindAllWithdrawRequest req) {
    var domainReq = toDomainReq(req);
    return service.getWithdraws(domainReq)
        .map(resp -> ApiResponsePaginationWithdraw.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromWithdrawResponse).toList())
            .setPaginationMeta(toMeta(resp.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationWithdraw> findAllWithdrawByCardNumber(FindAllWithdrawByCardNumberRequest req) {
    var domainReq = FindAllWithdrawCardNumber.builder()
        .cardNumber(req.getCardNumber())
        .search(req.getSearch())
        .page(req.getPage() > 0 ? req.getPage() : 1)
        .pageSize(req.getPageSize() > 0 ? req.getPageSize() : 10)
        .build();

    return service.getWithdrawsByCardNumber(domainReq)
        .map(resp -> ApiResponsePaginationWithdraw.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromWithdrawResponse).toList())
            .setPaginationMeta(toMeta(resp.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdraw> findByIdWithdraw(FindByIdWithdrawRequest req) {
    return service.getWithdrawById(req.getWithdrawId())
        .map(data -> ApiResponseWithdraw.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromWithdrawResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsesWithdraw> findByCardNumber(pb.card.Card.FindByCardNumberRequest req) {
    return service.getWithdrawsByCardNumberPrimitive(req.getCardNumber())
        .map(data -> ApiResponsesWithdraw.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(data.stream().map(ProtoConverter::fromWithdrawResponse).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationWithdrawDeleteAt> findByActive(FindAllWithdrawRequest req) {
    var domainReq = toDomainReq(req);
    return service.getActiveWithdraws(domainReq)
        .map(resp -> ApiResponsePaginationWithdrawDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromWithdrawResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationWithdrawDeleteAt> findByTrashed(FindAllWithdrawRequest req) {
    var domainReq = toDomainReq(req);
    return service.getTrashedWithdraws(domainReq)
        .map(resp -> ApiResponsePaginationWithdrawDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromWithdrawResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}