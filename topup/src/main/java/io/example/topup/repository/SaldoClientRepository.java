package io.example.topup.repository;

import io.example.common.exception.api.NotFoundException;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.saldo.Saldo.ApiResponseSaldo;
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

  /**
   * Atomically applies a delta to the card balance via the saldo service.
   * Positive delta credits, negative delta debits (guarded, never negative).
   */
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
}
