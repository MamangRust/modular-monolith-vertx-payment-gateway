package io.example.saldo.handler;

import java.time.LocalDateTime;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.saldo.service.SaldoCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.saldo.Saldo.ApiResponseSaldoDeleteAt;
import pb.saldo.Saldo.FindByIdSaldoRequest;
import pb.saldo.SaldoCommand.ApiResponseSaldoAll;
import pb.saldo.SaldoCommand.ApiResponseSaldoDelete;
import pb.saldo.SaldoCommand.CreateSaldoRequest;
import pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest;
import pb.saldo.SaldoCommand.UpdateSaldoDeltaRequest;
import pb.saldo.SaldoCommand.UpdateSaldoRequest;
import pb.saldo.SaldoCommand.UpdateSaldoWithdrawRequest;

@RequiredArgsConstructor
public class SaldoCommandHandler implements pb.saldo.VertxSaldoCommandServiceGrpcServer.SaldoCommandServiceApi {
  private final SaldoCommandService service;

  @Override
  public Future<ApiResponseSaldo> createSaldo(CreateSaldoRequest req) {
    var domainReq = io.example.saldo.domain.requests.CreateSaldoRequest.builder()
        .cardNumber(req.getCardNumber())
        .totalBalance((long) req.getTotalBalance())
        .build();

    return service.createSaldo(domainReq)
        .map(data -> ApiResponseSaldo.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromSaldoResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseSaldo> updateSaldo(UpdateSaldoRequest req) {
    var domainReq = io.example.saldo.domain.requests.UpdateSaldoRequest.builder()
        .saldoId(req.getSaldoId())
        .cardNumber(req.getCardNumber())
        .totalBalance((long) req.getTotalBalance())
        .build();

    return service.updateSaldo(domainReq)
        .map(data -> ApiResponseSaldo.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromSaldoResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseSaldo> updateSaldoBalance(UpdateSaldoBalanceRequest req) {
    var domainReq = io.example.saldo.domain.requests.UpdateSaldoBalanceRequest.builder()
        .cardNumber(req.getCardNumber())
        .totalBalance((long) req.getTotalBalance())
        .build();

    return service.updateSaldoBalance(domainReq)
        .map(data -> ApiResponseSaldo.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromSaldoResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseSaldo> updateSaldoDelta(UpdateSaldoDeltaRequest req) {
    var domainReq = io.example.saldo.domain.requests.UpdateSaldoDeltaRequest.builder()
        .cardNumber(req.getCardNumber())
        .delta((long) req.getDelta())
        .build();

    return service.updateSaldoDelta(domainReq)
        .map(data -> ApiResponseSaldo.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromSaldoResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseSaldo> updateSaldoWithdraw(UpdateSaldoWithdrawRequest req) {
    var domainReq = io.example.saldo.domain.requests.UpdateSaldoWithdrawRequest.builder()
        .cardNumber(req.getCardNumber())
        .withdrawAmount((long) req.getWithdrawAmount())
        .withdrawTime(LocalDateTime.parse(req.getWithdrawTime()))
        .build();

    return service.updateSaldoWithdraw(domainReq)
        .map(data -> ApiResponseSaldo.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromSaldoResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseSaldoDeleteAt> trashedSaldo(FindByIdSaldoRequest req) {
    return service.trashSaldo(req.getSaldoId())
        .map(data -> ApiResponseSaldoDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromSaldoResponseDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseSaldoDeleteAt> restoreSaldo(FindByIdSaldoRequest req) {
    return service.restoreSaldo(req.getSaldoId())
        .map(data -> ApiResponseSaldoDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromSaldoResponseDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseSaldoDelete> deleteSaldoPermanent(FindByIdSaldoRequest req) {
    return service.deleteSaldoPermanently(req.getSaldoId())
        .map(v -> ApiResponseSaldoDelete.newBuilder()
            .setStatus("success")
            .setMessage("Saldo deleted permanently")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseSaldoAll> restoreAllSaldo(com.google.protobuf.Empty req) {
    return service.restoreAllSaldos()
        .map(v -> ApiResponseSaldoAll.newBuilder()
            .setStatus("success")
            .setMessage("All saldos restored successfully")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseSaldoAll> deleteAllSaldoPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllPermanentSaldos()
        .map(v -> ApiResponseSaldoAll.newBuilder()
            .setStatus("success")
            .setMessage("All saldos permanently deleted")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}