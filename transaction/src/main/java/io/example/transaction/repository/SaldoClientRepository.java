package io.example.transaction.repository;

import io.example.common.exception.api.NotFoundException;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest;
import pb.saldo.SaldoCommand.UpdateSaldoDeltaRequest;
import pb.saldo.VertxSaldoCommandServiceGrpcClient;
import pb.saldo.VertxSaldoQueryServiceGrpcClient;

@RequiredArgsConstructor
public class SaldoClientRepository {
  private final VertxSaldoQueryServiceGrpcClient queryStub;
  private final VertxSaldoCommandServiceGrpcClient commandStub;

  public Future<ApiResponseSaldo> getSaldoByCardNumber(String cardNumber) {
    return queryStub
        .findByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
        .compose(resp -> {
          if (resp.getStatus().equals("error")) {
            return Future.failedFuture(new NotFoundException(resp.getMessage()));
          }
          return Future.succeededFuture(resp);
        });
  }

  public Future<ApiResponseSaldo> updateSaldoDelta(String cardNumber, int delta) {
    return commandStub.updateSaldoDelta(UpdateSaldoDeltaRequest.newBuilder()
        .setCardNumber(cardNumber)
        .setDelta(delta)
        .build())
        .compose(resp -> {
          if (resp.getStatus().equals("error")) {
            return Future.failedFuture(new RuntimeException(resp.getMessage()));
          }
          return Future.succeededFuture(resp);
        });
  }

  public Future<ApiResponseSaldo> updateSaldoBalance(String cardNumber, int newBalance) {
    return commandStub.updateSaldoBalance(UpdateSaldoBalanceRequest.newBuilder()
        .setCardNumber(cardNumber)
        .setTotalBalance(newBalance)
        .build())
        .compose(resp -> {
          if (resp.getStatus().equals("error")) {
            return Future.failedFuture(new RuntimeException(resp.getMessage()));
          }
          return Future.succeededFuture(resp);
        });
  }
}
