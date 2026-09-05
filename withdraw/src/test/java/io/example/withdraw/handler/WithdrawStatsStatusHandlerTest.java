package io.example.withdraw.handler;

import io.example.withdraw.model.WithdrawStats;
import io.example.withdraw.service.WithdrawStatsStatusService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.withdraw.Withdraw.FindMonthlyWithdrawStatus;
import pb.withdraw.Withdraw.FindMonthlyWithdrawStatusCardNumber;
import pb.withdraw.Withdraw.FindYearWithdrawStatus;
import pb.withdraw.Withdraw.FindYearWithdrawStatusCardNumber;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class WithdrawStatsStatusHandlerTest {

  @Mock
  private WithdrawStatsStatusService service;

  private WithdrawStatsStatusHandler handler;

  @BeforeEach
  void setUp() {
    handler = new WithdrawStatsStatusHandler(service);
  }

  @Test
  @DisplayName("findMonthlyWithdrawStatusSuccess success")
  void findMonthlyWithdrawStatusSuccess(VertxTestContext ctx) {
    List<WithdrawStats.MonthStatus> list = List.of(
        new WithdrawStats.MonthStatus("2026", "01", 10L, 500_000L));
    when(service.getMonthlyWithdrawStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = FindMonthlyWithdrawStatus.newBuilder().setYear(2026).setMonth(1).build();
    handler.findMonthlyWithdrawStatusSuccess(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalSuccess()).isEqualTo(10);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(500_000);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyWithdrawStatusSuccess success")
  void findYearlyWithdrawStatusSuccess(VertxTestContext ctx) {
    List<WithdrawStats.YearStatus> list = List.of(
        new WithdrawStats.YearStatus("2026", 120L, 6_000_000L));
    when(service.getYearlyWithdrawStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearWithdrawStatus.newBuilder().setYear(2026).build();
    handler.findYearlyWithdrawStatusSuccess(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalSuccess()).isEqualTo(120);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyWithdrawStatusFailed success")
  void findMonthlyWithdrawStatusFailed(VertxTestContext ctx) {
    List<WithdrawStats.MonthStatus> list = List.of(
        new WithdrawStats.MonthStatus("2026", "01", 3L, 50_000L));
    when(service.getMonthlyWithdrawStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = FindMonthlyWithdrawStatus.newBuilder().setYear(2026).setMonth(1).build();
    handler.findMonthlyWithdrawStatusFailed(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalFailed()).isEqualTo(3);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyWithdrawStatusFailed success")
  void findYearlyWithdrawStatusFailed(VertxTestContext ctx) {
    List<WithdrawStats.YearStatus> list = List.of(
        new WithdrawStats.YearStatus("2026", 15L, 200_000L));
    when(service.getYearlyWithdrawStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearWithdrawStatus.newBuilder().setYear(2026).build();
    handler.findYearlyWithdrawStatusFailed(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalFailed()).isEqualTo(15);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyWithdrawStatusSuccessCardNumber success")
  void findMonthlyWithdrawStatusSuccessCardNumber(VertxTestContext ctx) {
    List<WithdrawStats.MonthStatus> list = List.of(
        new WithdrawStats.MonthStatus("2026", "01", 5L, 250_000L));
    when(service.getMonthlyStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = FindMonthlyWithdrawStatusCardNumber.newBuilder()
        .setCardNumber("4111-1111").setYear(2026).setMonth(1).build();
    handler.findMonthlyWithdrawStatusSuccessCardNumber(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyWithdrawStatusSuccessCardNumber success")
  void findYearlyWithdrawStatusSuccessCardNumber(VertxTestContext ctx) {
    List<WithdrawStats.YearStatus> list = List.of(
        new WithdrawStats.YearStatus("2026", 60L, 3_000_000L));
    when(service.getYearlyStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearWithdrawStatusCardNumber.newBuilder()
        .setCardNumber("4111-1111").setYear(2026).build();
    handler.findYearlyWithdrawStatusSuccessCardNumber(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyWithdrawStatusFailedCardNumber success")
  void findMonthlyWithdrawStatusFailedCardNumber(VertxTestContext ctx) {
    List<WithdrawStats.MonthStatus> list = List.of(
        new WithdrawStats.MonthStatus("2026", "01", 2L, 30_000L));
    when(service.getMonthlyStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = FindMonthlyWithdrawStatusCardNumber.newBuilder()
        .setCardNumber("4111-1111").setYear(2026).setMonth(1).build();
    handler.findMonthlyWithdrawStatusFailedCardNumber(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyWithdrawStatusFailedCardNumber success")
  void findYearlyWithdrawStatusFailedCardNumber(VertxTestContext ctx) {
    List<WithdrawStats.YearStatus> list = List.of(
        new WithdrawStats.YearStatus("2026", 8L, 100_000L));
    when(service.getYearlyStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearWithdrawStatusCardNumber.newBuilder()
        .setCardNumber("4111-1111").setYear(2026).build();
    handler.findYearlyWithdrawStatusFailedCardNumber(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
