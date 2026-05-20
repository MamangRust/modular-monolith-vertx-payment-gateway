package io.example.transfer.handler;

import io.example.transfer.service.TransferCommandService;
import io.vertx.core.Future;
import pb.transfer.Transfer.*;
import pb.transfer.TransferCommand.*;

public class TransferCommandHandler implements pb.transfer.VertxTransferCommandServiceGrpcServer.TransferCommandServiceApi {
  private final TransferCommandService service;

  public TransferCommandHandler(TransferCommandService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseTransfer> createTransfer(CreateTransferRequest req) {
    return service.createTransfer(req)
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
  public Future<ApiResponseTransfer> updateTransfer(UpdateTransferRequest req) {
    return service.updateTransfer(req)
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
  public Future<ApIResponseTransferDeleteAt> trashedTransfer(FindByIdTransferRequest req) {
    return service.trashTransfer(req.getTransferId())
        .map(resp -> {
          var builder = ApIResponseTransferDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTransferResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApIResponseTransferDeleteAt> restoreTransfer(FindByIdTransferRequest req) {
    return service.restoreTransfer(req.getTransferId())
        .map(resp -> {
          var builder = ApIResponseTransferDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTransferResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTransferDelete> deleteTransferPermanent(FindByIdTransferRequest req) {
    return service.deleteTransferPermanently(req.getTransferId())
        .map(resp -> ApiResponseTransferDelete.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseTransferAll> restoreAllTransfer(com.google.protobuf.Empty req) {
    return service.restoreAllTransfers()
        .map(resp -> ApiResponseTransferAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseTransferAll> deleteAllTransferPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllPermanentTransfers()
        .map(resp -> ApiResponseTransferAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }
}
