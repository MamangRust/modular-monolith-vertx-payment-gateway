package io.example.card.handler;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.service.CardStatsTransactionService;
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
class CardStatsTransactionHandlerTest {
  @Mock private CardStatsTransactionService service;
  private CardStatsTransactionHandler handler;
  @BeforeEach void setUp() { handler = new CardStatsTransactionHandler(service); }

  @Test void monthly(VertxTestContext ctx) {
    when(service.getMonthlyTransactionAmount(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(1, 10000L))));
    handler.findMonthlyTransactionAmount(pb.card.Card.FindYearAmount.newBuilder().setYear(2026).build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow(); })));
  }
  @Test void yearly(VertxTestContext ctx) {
    when(service.getYearlyTransactionAmount(2026)).thenReturn(Future.succeededFuture(List.of(new CardStats.YearAmount(2025, 120000L))));
    handler.findYearlyTransactionAmount(pb.card.Card.FindYearAmount.newBuilder().setYear(2026).build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow(); })));
  }
  @Test void monthlyByCard(VertxTestContext ctx) {
    when(service.getMonthlyTransactionAmountByCardNumber(any(MonthYearCardNumberCard.class))).thenReturn(Future.succeededFuture(List.of(new CardStats.MonthAmount(3, 5000L))));
    handler.findMonthlyTransactionAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder().setYear(2026).setCardNumber("4111").build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r.getStatus()).isEqualTo("success"); ctx.completeNow(); })));
  }
}
