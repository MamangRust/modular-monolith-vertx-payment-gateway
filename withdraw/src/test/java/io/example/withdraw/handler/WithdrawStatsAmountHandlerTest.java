package io.example.withdraw.handler;

import io.example.withdraw.model.WithdrawStats;
import io.example.withdraw.service.WithdrawStatsAmountService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.withdraw.Withdraw.FindYearWithdrawCardNumber;
import pb.withdraw.Withdraw.FindYearWithdrawStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class WithdrawStatsAmountHandlerTest {

  @Mock
  private WithdrawStatsAmountService service;

  private WithdrawStatsAmountHandler handler;

  @BeforeEach
  void setUp() {
    handler = new WithdrawStatsAmountHandler(service);
  }

  @Test
  @DisplayName("findMonthlyWithdraws success")
  void findMonthlyWithdrawsSuccess(VertxTestContext ctx) {
    List<WithdrawStats.MonthAmount> list = List.of(new WithdrawStats.MonthAmount("Jan", 100L));
    when(service.getMonthlyWithdrawAmounts(2026)).thenReturn(Future.succeededFuture(list));

    var req = FindYearWithdrawStatus.newBuilder().setYear(2026).build();
    handler.findMonthlyWithdraws(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(100);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyWithdraws success")
  void findYearlyWithdrawsSuccess(VertxTestContext ctx) {
    List<WithdrawStats.YearAmount> list = List.of(new WithdrawStats.YearAmount("2026", 1000L));
    when(service.getYearlyWithdrawAmounts(2026)).thenReturn(Future.succeededFuture(list));

    var req = FindYearWithdrawStatus.newBuilder().setYear(2026).build();
    handler.findYearlyWithdraws(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(1000);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyWithdrawsByCardNumber success")
  void findMonthlyWithdrawsByCardNumberSuccess(VertxTestContext ctx) {
    List<WithdrawStats.MonthAmount> list = List.of(new WithdrawStats.MonthAmount("Feb", 200L));
    when(service.getMonthlyWithdrawAmountsByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearWithdrawCardNumber.newBuilder().setCardNumber("4111-1111").setYear(2026).build();
    handler.findMonthlyWithdrawsByCardNumber(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyWithdrawsByCardNumber success")
  void findYearlyWithdrawsByCardNumberSuccess(VertxTestContext ctx) {
    List<WithdrawStats.YearAmount> list = List.of(new WithdrawStats.YearAmount("2026", 2000L));
    when(service.getYearlyWithdrawAmountsByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearWithdrawCardNumber.newBuilder().setCardNumber("4111-1111").setYear(2026).build();
    handler.findYearlyWithdrawsByCardNumber(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }
}
