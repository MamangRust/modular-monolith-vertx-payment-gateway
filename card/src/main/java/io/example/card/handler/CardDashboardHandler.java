package io.example.card.handler;

import io.example.card.service.CardStatsDashboardService;
import io.vertx.core.Future;
import pb.card.CardDashboard.*;

public class CardDashboardHandler implements pb.card.VertxCardDashboardServiceGrpcServer.CardDashboardServiceApi {
  private final CardStatsDashboardService service;

  public CardDashboardHandler(CardStatsDashboardService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseDashboardCard> dashboardCard(com.google.protobuf.Empty req) {
    return service.getDashboardCard()
        .map(res -> ApiResponseDashboardCard.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .setData(CardResponseDashboard.newBuilder()
                .setTotalBalance(res.data().getTotalBalance())
                .setTotalTopup(res.data().getTotalTopup())
                .setTotalWithdraw(res.data().getTotalWithdraw())
                .setTotalTransaction(res.data().getTotalTransaction())
                .setTotalTransfer(res.data().getTotalTransfer())
                .build())
            .build());
  }

  @Override
  public Future<ApiResponseDashboardCardNumber> dashboardCardNumber(pb.card.Card.FindByCardNumberRequest req) {
    return service.getDashboardCardByCardNumber(req.getCardNumber())
        .map(res -> ApiResponseDashboardCardNumber.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .setData(CardResponseDashboardCardNumber.newBuilder()
                .setTotalBalance(res.data().getTotalBalance())
                .setTotalTopup(res.data().getTotalTopup())
                .setTotalWithdraw(res.data().getTotalWithdraw())
                .setTotalTransaction(res.data().getTotalTransaction())
                .setTotalTransferSend(res.data().getTotalTransferSend())
                .setTotalTransferReceiver(res.data().getTotalTransferReceiver())
                .build())
            .build());
  }
}
