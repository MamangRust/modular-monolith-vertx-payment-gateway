package io.example.topup.handler;

import io.example.topup.model.TopupStats;
import io.example.topup.service.TopupStatsStatusService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.topup.stats.TopupStatsStatus.ApiResponseTopupMonthStatusFailed;
import pb.topup.stats.TopupStatsStatus.ApiResponseTopupMonthStatusSuccess;
import pb.topup.stats.TopupStatsStatus.ApiResponseTopupYearStatusFailed;
import pb.topup.stats.TopupStatsStatus.ApiResponseTopupYearStatusSuccess;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TopupStatsStatusHandlerTest {

  @Mock
  private TopupStatsStatusService service;

  private TopupStatsStatusHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TopupStatsStatusHandler(service);
  }

  /* ─── Monthly success ─── */

  @Test
  @DisplayName("findMonthlyTopupStatusSuccess returns aggregated data")
  void findMonthlyTopupStatusSuccess(VertxTestContext ctx) {
    List<TopupStats.MonthStatus> list = List.of(
        new TopupStats.MonthStatus("2026", "Jan", 5L, 500L));
    when(service.getMonthlyTopupStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindMonthlyTopupStatus.newBuilder()
        .setYear(2026).setMonth(6).build();

    handler.findMonthlyTopupStatusSuccess(req)
        .<ApiResponseTopupMonthStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalSuccess()).isEqualTo(5);
          ctx.completeNow();
        })));
  }

  /* ─── Yearly success ─── */

  @Test
  @DisplayName("findYearlyTopupStatusSuccess returns aggregated data")
  void findYearlyTopupStatusSuccess(VertxTestContext ctx) {
    List<TopupStats.YearStatus> list = List.of(
        new TopupStats.YearStatus("2026", 50L, 5000L));
    when(service.getYearlyTopupStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupStatus.newBuilder().setYear(2026).build();

    handler.findYearlyTopupStatusSuccess(req)
        .<ApiResponseTopupYearStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalSuccess()).isEqualTo(50);
          ctx.completeNow();
        })));
  }

  /* ─── Monthly failed ─── */

  @Test
  @DisplayName("findMonthlyTopupStatusFailed returns aggregated data")
  void findMonthlyTopupStatusFailed(VertxTestContext ctx) {
    List<TopupStats.MonthStatus> list = List.of(
        new TopupStats.MonthStatus("2026", "Jan", 2L, 100L));
    when(service.getMonthlyTopupStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindMonthlyTopupStatus.newBuilder()
        .setYear(2026).setMonth(6).build();

    handler.findMonthlyTopupStatusFailed(req)
        .<ApiResponseTopupMonthStatusFailed>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalFailed()).isEqualTo(2);
          ctx.completeNow();
        })));
  }

  /* ─── Yearly failed ─── */

  @Test
  @DisplayName("findYearlyTopupStatusFailed returns aggregated data")
  void findYearlyTopupStatusFailed(VertxTestContext ctx) {
    List<TopupStats.YearStatus> list = List.of(
        new TopupStats.YearStatus("2026", 10L, 500L));
    when(service.getYearlyTopupStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupStatus.newBuilder().setYear(2026).build();

    handler.findYearlyTopupStatusFailed(req)
        .<ApiResponseTopupYearStatusFailed>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalFailed()).isEqualTo(10);
          ctx.completeNow();
        })));
  }

  /* ─── Monthly success by card number ─── */

  @Test
  @DisplayName("findMonthlyTopupStatusSuccessByCardNumber returns aggregated data")
  void findMonthlyTopupStatusSuccessByCardNumber(VertxTestContext ctx) {
    List<TopupStats.MonthStatus> list = List.of(
        new TopupStats.MonthStatus("2026", "Jan", 3L, 300L));
    when(service.getMonthlyTopupStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindMonthlyTopupStatusCardNumber.newBuilder()
        .setYear(2026).setMonth(6).setCardNumber("4111111111111111").build();

    handler.findMonthlyTopupStatusSuccessByCardNumber(req)
        .<ApiResponseTopupMonthStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── Yearly success by card number ─── */

  @Test
  @DisplayName("findYearlyTopupStatusSuccessByCardNumber returns aggregated data")
  void findYearlyTopupStatusSuccessByCardNumber(VertxTestContext ctx) {
    List<TopupStats.YearStatus> list = List.of(
        new TopupStats.YearStatus("2026", 20L, 2000L));
    when(service.getYearlyTopupStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupStatusCardNumber.newBuilder()
        .setYear(2026).setCardNumber("4111111111111111").build();

    handler.findYearlyTopupStatusSuccessByCardNumber(req)
        .<ApiResponseTopupYearStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── Monthly failed by card number ─── */

  @Test
  @DisplayName("findMonthlyTopupStatusFailedByCardNumber returns aggregated data")
  void findMonthlyTopupStatusFailedByCardNumber(VertxTestContext ctx) {
    List<TopupStats.MonthStatus> list = List.of(
        new TopupStats.MonthStatus("2026", "Jan", 1L, 50L));
    when(service.getMonthlyTopupStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindMonthlyTopupStatusCardNumber.newBuilder()
        .setYear(2026).setMonth(6).setCardNumber("4111111111111111").build();

    handler.findMonthlyTopupStatusFailedByCardNumber(req)
        .<ApiResponseTopupMonthStatusFailed>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── Yearly failed by card number ─── */

  @Test
  @DisplayName("findYearlyTopupStatusFailedByCardNumber returns aggregated data")
  void findYearlyTopupStatusFailedByCardNumber(VertxTestContext ctx) {
    List<TopupStats.YearStatus> list = List.of(
        new TopupStats.YearStatus("2026", 5L, 100L));
    when(service.getYearlyTopupStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupStatusCardNumber.newBuilder()
        .setYear(2026).setCardNumber("4111111111111111").build();

    handler.findYearlyTopupStatusFailedByCardNumber(req)
        .<ApiResponseTopupYearStatusFailed>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
