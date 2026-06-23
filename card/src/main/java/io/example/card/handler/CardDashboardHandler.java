package io.example.card.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.card.service.CardStatsDashboardService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.CardDashboard.*;

@RequiredArgsConstructor
public class CardDashboardHandler implements pb.card.VertxCardDashboardServiceGrpcServer.CardDashboardServiceApi {
  private final CardStatsDashboardService service;

  @Override
  public Future<ApiResponseDashboardCard> dashboardCard(com.google.protobuf.Empty req) {
    return service.getDashboardCard()
        .map(dash -> ApiResponseDashboardCard.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(CardResponseDashboard.newBuilder()
                .setTotalBalance(dash.getTotalBalance())
                .setTotalTopup(dash.getTotalTopup())
                .setTotalWithdraw(dash.getTotalWithdraw())
                .setTotalTransaction(dash.getTotalTransaction())
                .setTotalTransfer(dash.getTotalTransfer())
                .build())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseDashboardCardNumber> dashboardCardNumber(pb.card.Card.FindByCardNumberRequest req) {
    return service.getDashboardCardByCardNumber(req.getCardNumber())
        .map(dash -> ApiResponseDashboardCardNumber.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(CardResponseDashboardCardNumber.newBuilder()
                .setTotalBalance(dash.getTotalBalance())
                .setTotalTopup(dash.getTotalTopup())
                .setTotalWithdraw(dash.getTotalWithdraw())
                .setTotalTransaction(dash.getTotalTransaction())
                .setTotalTransferSend(dash.getTotalTransferSend())
                .setTotalTransferReceiver(dash.getTotalTransferReceiver())
                .build())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}