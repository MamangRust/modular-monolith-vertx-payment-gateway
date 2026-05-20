package io.example.transaction.handler;

import io.example.transaction.service.TransactionCommandService;
import io.vertx.core.Future;
import pb.transaction.Transaction.*;
import pb.transaction.TransactionCommand.*;

public class TransactionCommandHandler implements pb.transaction.VertxTransactionCommandServiceGrpcServer.TransactionCommandServiceApi {
  private final TransactionCommandService service;

  public TransactionCommandHandler(TransactionCommandService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseTransaction> createTransaction(CreateTransactionRequest req) {
    return service.createTransaction(req)
        .map(resp -> {
          var builder = ApiResponseTransaction.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTransactionResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTransaction> updateTransaction(UpdateTransactionRequest req) {
    return service.updateTransaction(req)
        .map(resp -> {
          var builder = ApiResponseTransaction.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTransactionResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTransactionDeleteAt> trashedTransaction(FindByIdTransactionRequest req) {
    return service.trashTransaction(req.getTransactionId())
        .map(resp -> {
          var builder = ApiResponseTransactionDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTransactionResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTransactionDeleteAt> restoreTransaction(FindByIdTransactionRequest req) {
    return service.restoreTransaction(req.getTransactionId())
        .map(resp -> {
          var builder = ApiResponseTransactionDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTransactionResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTransactionDelete> deleteTransactionPermanent(FindByIdTransactionRequest req) {
    return service.deleteTransactionPermanently(req.getTransactionId())
        .map(resp -> ApiResponseTransactionDelete.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseTransactionAll> restoreAllTransaction(com.google.protobuf.Empty req) {
    return service.restoreAllTransactions()
        .map(resp -> ApiResponseTransactionAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseTransactionAll> deleteAllTransactionPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllPermanentTransactions()
        .map(resp -> ApiResponseTransactionAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }
}
