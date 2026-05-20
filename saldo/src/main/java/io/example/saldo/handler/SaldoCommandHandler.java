package io.example.saldo.handler;

import java.time.LocalDateTime;

import io.example.saldo.service.SaldoCommandService;
import io.vertx.core.Future;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.saldo.Saldo.ApiResponseSaldoDeleteAt;
import pb.saldo.Saldo.FindByIdSaldoRequest;
import pb.saldo.SaldoCommand.ApiResponseSaldoAll;
import pb.saldo.SaldoCommand.ApiResponseSaldoDelete;
import pb.saldo.SaldoCommand.CreateSaldoRequest;
import pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest;
import pb.saldo.SaldoCommand.UpdateSaldoRequest;
import pb.saldo.SaldoCommand.UpdateSaldoWithdrawRequest;

public class SaldoCommandHandler implements pb.saldo.VertxSaldoCommandServiceGrpcServer.SaldoCommandServiceApi {
  private final SaldoCommandService service;

  public SaldoCommandHandler(SaldoCommandService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseSaldo> createSaldo(CreateSaldoRequest req) {
    io.example.saldo.domain.requests.CreateSaldoRequest domainReq = io.example.saldo.domain.requests.CreateSaldoRequest
        .builder()
        .cardNumber(req.getCardNumber())
        .totalBalance((long) req.getTotalBalance())
        .build();

    return service.createSaldo(domainReq)
        .map(resp -> {
          var builder = ApiResponseSaldo.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromSaldoResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseSaldo> updateSaldo(UpdateSaldoRequest req) {
    io.example.saldo.domain.requests.UpdateSaldoRequest domainReq = io.example.saldo.domain.requests.UpdateSaldoRequest
        .builder()
        .saldoId(req.getSaldoId())
        .cardNumber(req.getCardNumber())
        .totalBalance((long) req.getTotalBalance())
        .build();

    return service.updateSaldo(domainReq)
        .map(resp -> {
          var builder = ApiResponseSaldo.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromSaldoResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseSaldo> updateSaldoBalance(UpdateSaldoBalanceRequest req) {
    io.example.saldo.domain.requests.UpdateSaldoBalanceRequest domainReq = io.example.saldo.domain.requests.UpdateSaldoBalanceRequest
        .builder()
        .cardNumber(req.getCardNumber())
        .totalBalance((long) req.getTotalBalance())
        .build();

    return service.updateSaldoBalance(domainReq)
        .map(resp -> {
          var builder = ApiResponseSaldo.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromSaldoResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseSaldo> updateSaldoWithdraw(UpdateSaldoWithdrawRequest req) {
    io.example.saldo.domain.requests.UpdateSaldoWithdrawRequest domainReq = io.example.saldo.domain.requests.UpdateSaldoWithdrawRequest
        .builder()
        .cardNumber(req.getCardNumber())
        .withdrawAmount((long) req.getWithdrawAmount())
        .withdrawTime(LocalDateTime.parse(req.getWithdrawTime()))
        .build();

    return service.updateSaldoWithdraw(domainReq)
        .map(resp -> {
          var builder = ApiResponseSaldo.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromSaldoResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseSaldoDeleteAt> trashedSaldo(FindByIdSaldoRequest req) {
    return service.trashSaldo(req.getSaldoId())
        .map(resp -> {
          var builder = ApiResponseSaldoDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromSaldoResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseSaldoDeleteAt> restoreSaldo(FindByIdSaldoRequest req) {
    return service.restoreSaldo(req.getSaldoId())
        .map(resp -> {
          var builder = ApiResponseSaldoDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromSaldoResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseSaldoDelete> deleteSaldoPermanent(FindByIdSaldoRequest req) {
    return service.deleteSaldoPermanently(req.getSaldoId())
        .map(resp -> ApiResponseSaldoDelete.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseSaldoAll> restoreAllSaldo(com.google.protobuf.Empty req) {
    return service.restoreAllSaldos()
        .map(resp -> ApiResponseSaldoAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseSaldoAll> deleteAllSaldoPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllPermanentSaldos()
        .map(resp -> ApiResponseSaldoAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }
}
