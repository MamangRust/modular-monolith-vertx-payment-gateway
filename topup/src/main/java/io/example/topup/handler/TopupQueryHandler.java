package io.example.topup.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.topup.domain.requests.topup.FindAllTopups;
import io.example.topup.domain.requests.topup.FindAllTopupsByCardNumber;
import io.example.topup.service.TopupQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.topup.Topup.ApiResponseTopup;
import pb.topup.Topup.FindByCardNumberTopupRequest;
import pb.topup.Topup.FindByIdTopupRequest;
import pb.topup.TopupQuery.ApiResponsePaginationTopup;
import pb.topup.TopupQuery.ApiResponsePaginationTopupDeleteAt;
import pb.topup.TopupQuery.FindAllTopupByCardNumberRequest;
import pb.topup.TopupQuery.FindAllTopupRequest;

@RequiredArgsConstructor
public class TopupQueryHandler implements pb.topup.VertxTopupQueryServiceGrpcServer.TopupQueryServiceApi {
  private final TopupQueryService service;

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

  private FindAllTopups toDomainReq(FindAllTopupRequest req) {
    return FindAllTopups.builder()
        .search(req.getSearch())
        .page(req.getPage() > 0 ? req.getPage() : 1)
        .pageSize(req.getPageSize() > 0 ? req.getPageSize() : 10)
        .build();
  }

  @Override
  public Future<ApiResponsePaginationTopup> findAllTopup(FindAllTopupRequest req) {
    var domainReq = toDomainReq(req);
    return service.getTopups(domainReq)
        .map(resp -> ApiResponsePaginationTopup.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromTopupResponse).toList())
            .setPaginationMeta(toMeta(resp.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationTopup> findAllTopupByCardNumber(FindAllTopupByCardNumberRequest req) {
    var domainReq = FindAllTopupsByCardNumber.builder()
        .cardNumber(req.getCardNumber())
        .search(req.getSearch())
        .page(req.getPage() > 0 ? req.getPage() : 1)
        .pageSize(req.getPageSize() > 0 ? req.getPageSize() : 10)
        .build();

    return service.getTopupsByCardNumber(domainReq)
        .map(resp -> ApiResponsePaginationTopup.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromTopupResponse).toList())
            .setPaginationMeta(toMeta(resp.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopup> findByIdTopup(FindByIdTopupRequest req) {
    return service.getTopupById(req.getTopupId())
        .map(data -> ApiResponseTopup.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTopupResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopup> findByCardNumberTopup(FindByCardNumberTopupRequest req) {
    return service.getTopupByCardNumber(req.getCardNumber())
        .map(data -> ApiResponseTopup.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTopupResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationTopupDeleteAt> findByActive(FindAllTopupRequest req) {
    var domainReq = toDomainReq(req);
    return service.getActiveTopups(domainReq)
        .<ApiResponsePaginationTopupDeleteAt>map(resp -> ApiResponsePaginationTopupDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromTopupResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationTopupDeleteAt> findByTrashed(FindAllTopupRequest req) {
    var domainReq = toDomainReq(req);
    return service.getTrashedTopups(domainReq)
        .map(resp -> ApiResponsePaginationTopupDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromTopupResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}