package io.example.card.handler;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.service.CardStatsTransferService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardStatsTransferHandlerTest {
  @Mock private CardStatsTransferService service;
  private CardStatsTransferHandler handler;
  @BeforeEach void setUp() { handler = new CardStatsTransferHandler(service); }

  @Test void monthlySender(VertxTestContext ctx) {
    when(service.getMonthlyTransferAmountSender(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(1, 5000L))));
    handler.findMonthlyTransferSenderAmount(pb.card.Card.FindYearAmount.newBuilder().setYear(2026).build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow(); })));
  }
  @Test void monthlyReceiver(VertxTestContext ctx) {
    when(service.getMonthlyTransferAmountReceiver(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(1, 3000L))));
    handler.findMonthlyTransferReceiverAmount(pb.card.Card.FindYearAmount.newBuilder().setYear(2026).build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow(); })));
  }
  @Test void monthlyByCardSender(VertxTestContext ctx) {
    when(service.getMonthlyTransferAmountBySender(any(MonthYearCardNumberCard.class))).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(3, 4000L))));
    handler.findMonthlyTransferSenderAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder().setYear(2026).setCardNumber("4111").build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow(); })));
  }
  @Test void monthlyByCardReceiver(VertxTestContext ctx) {
    when(service.getMonthlyTransferAmountByReceiver(any(MonthYearCardNumberCard.class))).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(3, 2000L))));
    handler.findMonthlyTransferReceiverAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder().setYear(2026).setCardNumber("4111").build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow(); })));
  }
}
