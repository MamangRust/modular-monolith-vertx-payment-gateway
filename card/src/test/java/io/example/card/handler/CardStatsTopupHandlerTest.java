package io.example.card.handler;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.service.CardStatsTopupService;
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
class CardStatsTopupHandlerTest {
  @Mock private CardStatsTopupService service;
  private CardStatsTopupHandler handler;
  @BeforeEach void setUp() { handler = new CardStatsTopupHandler(service); }

  @Test void monthly(VertxTestContext ctx) {
    when(service.getMonthlyTopupAmount(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(1, 5000L))));
    handler.findMonthlyTopupAmount(pb.card.Card.FindYearAmount.newBuilder().setYear(2026).build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getStatus()).isEqualTo("success"); assertThat(r.getDataList()).hasSize(1); ctx.completeNow(); })));
  }

  @Test void yearly(VertxTestContext ctx) {
    when(service.getYearlyTopupAmount(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.YearAmount(2025, 60000L))));
    handler.findYearlyTopupAmount(pb.card.Card.FindYearAmount.newBuilder().setYear(2026).build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow(); })));
  }

  @Test void monthlyByCard(VertxTestContext ctx) {
    when(service.getMonthlyTopupAmountByCardNumber(any(MonthYearCardNumberCard.class))).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(3, 3000L))));
    handler.findMonthlyTopupAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder().setYear(2026).setCardNumber("4111").build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow(); })));
  }
}
