package io.example.transfer.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transfer.service.TransferCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transfer.Transfer.ApIResponseTransferDeleteAt;
import pb.transfer.Transfer.ApiResponseTransfer;
import pb.transfer.Transfer.FindByIdTransferRequest;
import pb.transfer.TransferCommand.ApiResponseTransferAll;
import pb.transfer.TransferCommand.ApiResponseTransferDelete;
import pb.transfer.TransferCommand.CreateTransferRequest;
import pb.transfer.TransferCommand.UpdateTransferRequest;

@RequiredArgsConstructor
public class TransferCommandHandler
    implements pb.transfer.VertxTransferCommandServiceGrpcServer.TransferCommandServiceApi {
  private final TransferCommandService service;

  @Override
  public Future<ApiResponseTransfer> createTransfer(CreateTransferRequest req) {
    return service.createTransfer(req)
        .map(data -> ApiResponseTransfer.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTransferResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransfer> updateTransfer(UpdateTransferRequest req) {
    return service.updateTransfer(req)
        .map(data -> ApiResponseTransfer.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTransferResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApIResponseTransferDeleteAt> trashedTransfer(FindByIdTransferRequest req) {
    return service.trashTransfer(req.getTransferId())
        .map(data -> ApIResponseTransferDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTransferResponseDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApIResponseTransferDeleteAt> restoreTransfer(FindByIdTransferRequest req) {
    return service.restoreTransfer(req.getTransferId())
        .map(data -> ApIResponseTransferDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTransferResponseDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferDelete> deleteTransferPermanent(FindByIdTransferRequest req) {
    return service.deleteTransferPermanently(req.getTransferId())
        .map(v -> ApiResponseTransferDelete.newBuilder()
            .setStatus("success")
            .setMessage("Transfer deleted permanently")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferAll> restoreAllTransfer(com.google.protobuf.Empty req) {
    return service.restoreAllTransfers()
        .map(v -> ApiResponseTransferAll.newBuilder()
            .setStatus("success")
            .setMessage("All transfers restored successfully")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransferAll> deleteAllTransferPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllPermanentTransfers()
        .map(v -> ApiResponseTransferAll.newBuilder()
            .setStatus("success")
            .setMessage("All trashed transfers deleted permanently")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}