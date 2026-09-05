package io.example.card.handler;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.service.CardStatsBalanceService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.card.stats.CardStatsBalance.FindYearBalance;
import pb.card.stats.CardStatsBalance.FindYearBalanceCardNumber;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardStatsBalanceHandlerTest {

  @Mock
  private CardStatsBalanceService service;

  private CardStatsBalanceHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CardStatsBalanceHandler(service);
  }

  /* ─── findMonthlyBalance ─── */

  @Test
  @DisplayName("findMonthlyBalance delegates and returns response")
  void findMonthlyBalance(VertxTestContext ctx) {
    var data = List.of(
        new CardStats.MonthBalance(1, 1000L),
        new CardStats.MonthBalance(2, 2000L));

    when(service.getMonthlyBalances(2026)).thenReturn(Future.succeededFuture(data));

    var req = FindYearBalance.newBuilder().setYear(2026).build();

    handler.findMonthlyBalance(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataList()).hasSize(2);
          assertThat(resp.getDataList().get(0).getMonth()).isEqualTo("1");
          assertThat(resp.getDataList().get(0).getTotalBalance()).isEqualTo(1000L);
          ctx.completeNow();
        })));
  }

  /* ─── findYearlyBalance ─── */

  @Test
  @DisplayName("findYearlyBalance delegates and returns response")
  void findYearlyBalance(VertxTestContext ctx) {
    var data = List.of(
        new CardStats.YearlyBalance(2025, 50000L),
        new CardStats.YearlyBalance(2026, 60000L));

    when(service.getYearlyBalances(2026)).thenReturn(Future.succeededFuture(data));

    var req = FindYearBalance.newBuilder().setYear(2026).build();

    handler.findYearlyBalance(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataList()).hasSize(2);
          assertThat(resp.getDataList().get(0).getYear()).isEqualTo("2025");
          assertThat(resp.getDataList().get(0).getTotalBalance()).isEqualTo(50000L);
          ctx.completeNow();
        })));
  }

  /* ─── findMonthlyBalanceByCardNumber ─── */

  @Test
  @DisplayName("findMonthlyBalanceByCardNumber delegates and returns response")
  void findMonthlyBalanceByCardNumber(VertxTestContext ctx) {
    var data = List.of(new CardStats.MonthBalance(3, 3000L));

    when(service.getMonthlyBalancesByCardNumber(any(MonthYearCardNumberCard.class)))
        .thenReturn(Future.succeededFuture(data));

    var req = FindYearBalanceCardNumber.newBuilder()
        .setYear(2026).setCardNumber("4111111111111111").build();

    handler.findMonthlyBalanceByCardNumber(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataList()).hasSize(1);
          assertThat(resp.getDataList().get(0).getMonth()).isEqualTo("3");
          ctx.completeNow();
        })));
  }

  /* ─── findYearlyBalanceByCardNumber ─── */

  @Test
  @DisplayName("findYearlyBalanceByCardNumber delegates and returns response")
  void findYearlyBalanceByCardNumber(VertxTestContext ctx) {
    var data = List.of(new CardStats.YearlyBalance(2025, 40000L));

    when(service.getYearlyBalancesByCardNumber(any(MonthYearCardNumberCard.class)))
        .thenReturn(Future.succeededFuture(data));

    var req = FindYearBalanceCardNumber.newBuilder()
        .setYear(2026).setCardNumber("4111111111111111").build();

    handler.findYearlyBalanceByCardNumber(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataList()).hasSize(1);
          assertThat(resp.getDataList().get(0).getYear()).isEqualTo("2025");
          ctx.completeNow();
        })));
  }

  /* ─── error path ─── */

  @Test
  @DisplayName("findMonthlyBalance delegates error when service fails")
  void findMonthlyBalanceError(VertxTestContext ctx) {
    when(service.getMonthlyBalances(2026))
        .thenReturn(Future.failedFuture(new RuntimeException("DB error")));

    var req = FindYearBalance.newBuilder().setYear(2026).build();

    handler.findMonthlyBalance(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }
}
