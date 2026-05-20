package io.example.withdraw.repository;

import io.example.common.exception.NotFoundException;
import io.vertx.core.Future;
import pb.saldo.Saldo.*;
import pb.saldo.SaldoCommand.*;
import pb.saldo.VertxSaldoCommandServiceGrpcClient;
import pb.saldo.VertxSaldoQueryServiceGrpcClient;

public class SaldoClientRepository {
  private final VertxSaldoQueryServiceGrpcClient queryStub;
  private final VertxSaldoCommandServiceGrpcClient commandStub;

  public SaldoClientRepository(VertxSaldoQueryServiceGrpcClient queryStub, VertxSaldoCommandServiceGrpcClient commandStub) {
    this.queryStub = queryStub;
    this.commandStub = commandStub;
  }

  public Future<ApiResponseSaldo> getSaldoByCardNumber(String cardNumber) {
    return queryStub.findByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
        .compose(resp -> {
          if (resp.getStatus().equals("error")) {
            return Future.failedFuture(new NotFoundException(resp.getMessage()));
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
