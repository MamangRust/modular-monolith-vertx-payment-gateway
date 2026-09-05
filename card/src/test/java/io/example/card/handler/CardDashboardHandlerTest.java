package io.example.card.handler;

import com.google.protobuf.Empty;
import io.example.card.model.CardStats;
import io.example.card.service.CardStatsDashboardService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.card.Card.FindByCardNumberRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardDashboardHandlerTest {

  @Mock
  private CardStatsDashboardService service;

  private CardDashboardHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CardDashboardHandler(service);
  }

  /* ─── dashboardCard ─── */

  @Test
  @DisplayName("dashboardCard delegates and returns dashboard response")
  void dashboardCard(VertxTestContext ctx) {
    var dash = new CardStats.Dashboard(1000L, 200L, 50L, 300L, 150L);

    when(service.getDashboardCard()).thenReturn(Future.succeededFuture(dash));

    handler.dashboardCard(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getTotalBalance()).isEqualTo(1000L);
          assertThat(resp.getData().getTotalTopup()).isEqualTo(200L);
          assertThat(resp.getData().getTotalWithdraw()).isEqualTo(50L);
          assertThat(resp.getData().getTotalTransaction()).isEqualTo(300L);
          assertThat(resp.getData().getTotalTransfer()).isEqualTo(150L);
          ctx.completeNow();
        })));
  }

  /* ─── dashboardCardNumber ─── */

  @Test
  @DisplayName("dashboardCardNumber delegates and returns per-card dashboard response")
  void dashboardCardNumber(VertxTestContext ctx) {
    var dash = new CardStats.DashboardByCardNumber(500L, 100L, 25L, 150L, 50L, 30L);

    when(service.getDashboardCardByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(dash));

    var req = FindByCardNumberRequest.newBuilder()
        .setCardNumber("4111111111111111").build();

    handler.dashboardCardNumber(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getTotalBalance()).isEqualTo(500L);
          assertThat(resp.getData().getTotalTransferSend()).isEqualTo(50L);
          assertThat(resp.getData().getTotalTransferReceiver()).isEqualTo(30L);
          ctx.completeNow();
        })));
  }

  /* ─── error path ─── */

  @Test
  @DisplayName("dashboardCard delegates error when service fails")
  void dashboardCardError(VertxTestContext ctx) {
    when(service.getDashboardCard())
        .thenReturn(Future.failedFuture(new RuntimeException("DB error")));

    handler.dashboardCard(Empty.getDefaultInstance())
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }
}
