package io.example.transfer.handler;

import io.example.common.model.PaginationMeta;
import io.example.transfer.service.TransferQueryService;
import io.vertx.core.Future;
import pb.transfer.Transfer.*;
import pb.transfer.TransferQuery.*;

public class TransferQueryHandler implements pb.transfer.VertxTransferQueryServiceGrpcServer.TransferQueryServiceApi {
  private final TransferQueryService service;

  public TransferQueryHandler(TransferQueryService service) {
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
  public Future<ApiResponsePaginationTransfer> findAllTransfer(FindAllTransferRequest req) {
    return service.getAllTransfers(req)
        .map(resp -> ApiResponsePaginationTransfer.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromTransferResponse).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponseTransfer> findByIdTransfer(FindByIdTransferRequest req) {
    return service.getTransferById(req.getTransferId())
        .map(resp -> {
          var builder = ApiResponseTransfer.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTransferResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTransfers> findTransferByTransferFrom(FindTransferByTransferFromRequest req) {
    return service.getTransfersAsSender(req.getTransferFrom())
        .map(resp -> {
          var builder = ApiResponseTransfers.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.addAllData(resp.data().stream().map(ProtoConverter::fromTransferResponse).toList());
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTransfers> findTransferByTransferTo(FindTransferByTransferToRequest req) {
    return service.getTransfersAsReceiver(req.getTransferTo())
        .map(resp -> {
          var builder = ApiResponseTransfers.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.addAllData(resp.data().stream().map(ProtoConverter::fromTransferResponse).toList());
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponsePaginationTransferDeleteAt> findByActiveTransfer(FindAllTransferRequest req) {
    return service.getActiveTransfers(req)
        .map(resp -> ApiResponsePaginationTransferDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromTransferResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationTransferDeleteAt> findByTrashedTransfer(FindAllTransferRequest req) {
    return service.getTrashedTransfers(req)
        .map(resp -> ApiResponsePaginationTransferDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromTransferResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }
}
