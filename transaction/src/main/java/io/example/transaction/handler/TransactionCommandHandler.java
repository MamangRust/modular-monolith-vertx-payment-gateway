package io.example.transaction.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transaction.service.TransactionCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transaction.Transaction.ApiResponseTransaction;
import pb.transaction.Transaction.ApiResponseTransactionDeleteAt;
import pb.transaction.Transaction.FindByIdTransactionRequest;
import pb.transaction.TransactionCommand.ApiResponseTransactionAll;
import pb.transaction.TransactionCommand.ApiResponseTransactionDelete;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;

@RequiredArgsConstructor
public class TransactionCommandHandler
    implements pb.transaction.VertxTransactionCommandServiceGrpcServer.TransactionCommandServiceApi {
  private final TransactionCommandService service;

  @Override
  public Future<ApiResponseTransaction> createTransaction(CreateTransactionRequest req) {
    return service.createTransaction(req)
        .map(data -> ApiResponseTransaction.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTransactionResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransaction> updateTransaction(UpdateTransactionRequest req) {
    return service.updateTransaction(req)
        .map(data -> ApiResponseTransaction.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTransactionResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransactionDeleteAt> trashedTransaction(FindByIdTransactionRequest req) {
    return service.trashTransaction(req.getTransactionId())
        .map(data -> ApiResponseTransactionDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTransactionResponseDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransactionDeleteAt> restoreTransaction(FindByIdTransactionRequest req) {
    return service.restoreTransaction(req.getTransactionId())
        .map(data -> ApiResponseTransactionDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTransactionResponseDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransactionDelete> deleteTransactionPermanent(FindByIdTransactionRequest req) {
    return service.deleteTransactionPermanently(req.getTransactionId())
        .map(v -> ApiResponseTransactionDelete.newBuilder()
            .setStatus("success")
            .setMessage("Transaction deleted permanently")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransactionAll> restoreAllTransaction(com.google.protobuf.Empty req) {
    return service.restoreAllTransactions()
        .map(v -> ApiResponseTransactionAll.newBuilder()
            .setStatus("success")
            .setMessage("All transactions restored successfully")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTransactionAll> deleteAllTransactionPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllPermanentTransactions()
        .map(v -> ApiResponseTransactionAll.newBuilder()
            .setStatus("success")
            .setMessage("All transactions permanently deleted")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}