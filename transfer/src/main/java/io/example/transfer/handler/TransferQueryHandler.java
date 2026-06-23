package io.example.transfer.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transfer.domain.requests.FindAllTransfers;
import io.example.transfer.service.TransferQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transfer.Transfer.ApiResponseTransfer;
import pb.transfer.Transfer.FindAllTransferRequest;
import pb.transfer.Transfer.FindByIdTransferRequest;
import pb.transfer.Transfer.FindTransferByTransferFromRequest;
import pb.transfer.Transfer.FindTransferByTransferToRequest;
import pb.transfer.TransferQuery.ApiResponsePaginationTransfer;
import pb.transfer.TransferQuery.ApiResponsePaginationTransferDeleteAt;
import pb.transfer.TransferQuery.ApiResponseTransfers;

@RequiredArgsConstructor
public class TransferQueryHandler implements pb.transfer.VertxTransferQueryServiceGrpcServer.TransferQueryServiceApi {
  private final TransferQueryService service;

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

  private FindAllTransfers toDomainReq(FindAllTransferRequest req) {
    return FindAllTransfers.builder()
        .search(req.getSearch())
        .page(req.getPage() > 0 ? req.getPage() : 1)
        .pageSize(req.getPageSize() > 0 ? req.getPageSize() : 10)
        .build();
  }

  @Override
  public Future<ApiResponsePaginationTransfer> findAllTransfer(FindAllTransferRequest req) {
    var domainReq = toDomainReq(req);
    return service.getAllTransfers(domainReq)
        .map(resp -> ApiResponsePaginationTransfer.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromTransferResponse).toList())
            .setPaginationMeta(toMeta(resp.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransfer> findByIdTransfer(FindByIdTransferRequest req) {
    return service.getTransferById(req.getTransferId())
        .map(data -> ApiResponseTransfer.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTransferResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransfers> findTransferByTransferFrom(FindTransferByTransferFromRequest req) {
    return service.getTransfersAsSender(req.getTransferFrom())
        .map(data -> ApiResponseTransfers.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(data.stream().map(ProtoConverter::fromTransferResponse).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransfers> findTransferByTransferTo(FindTransferByTransferToRequest req) {
    return service.getTransfersAsReceiver(req.getTransferTo())
        .map(data -> ApiResponseTransfers.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(data.stream().map(ProtoConverter::fromTransferResponse).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationTransferDeleteAt> findByActiveTransfer(FindAllTransferRequest req) {
    var domainReq = toDomainReq(req);
    return service.getActiveTransfers(domainReq)
        .map(resp -> ApiResponsePaginationTransferDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromTransferResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationTransferDeleteAt> findByTrashedTransfer(FindAllTransferRequest req) {
    var domainReq = toDomainReq(req);
    return service.getTrashedTransfers(domainReq)
        .map(resp -> ApiResponsePaginationTransferDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromTransferResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}