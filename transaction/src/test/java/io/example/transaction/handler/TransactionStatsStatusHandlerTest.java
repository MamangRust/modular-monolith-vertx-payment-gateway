package io.example.transaction.handler;

import io.example.transaction.model.TransactionStats;
import io.example.transaction.service.TransactionStatsStatusService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed;
import pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess;
import pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusFailed;
import pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TransactionStatsStatusHandlerTest {

  @Mock
  private TransactionStatsStatusService service;

  private TransactionStatsStatusHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TransactionStatsStatusHandler(service);
  }

  /* ─── Monthly success ─── */

  @Test
  @DisplayName("findMonthlyTransactionStatusSuccess returns aggregated data")
  void findMonthlyTransactionStatusSuccess(VertxTestContext ctx) {
    List<TransactionStats.MonthStatus> list = List.of(
        new TransactionStats.MonthStatus("2026", "Jan", 5L, 500L));
    when(service.getMonthlyStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindMonthlyTransactionStatus.newBuilder()
        .setYear(2026).setMonth(6).build();

    handler.findMonthlyTransactionStatusSuccess(req)
        .<ApiResponseTransactionMonthStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalSuccess()).isEqualTo(5);
          ctx.completeNow();
        })));
  }

  /* ─── Yearly success ─── */

  @Test
  @DisplayName("findYearlyTransactionStatusSuccess returns aggregated data")
  void findYearlyTransactionStatusSuccess(VertxTestContext ctx) {
    List<TransactionStats.YearStatus> list = List.of(
        new TransactionStats.YearStatus("2026", 50L, 5000L));
    when(service.getYearlyStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindYearTransactionStatus.newBuilder().setYear(2026).build();

    handler.findYearlyTransactionStatusSuccess(req)
        .<ApiResponseTransactionYearStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalSuccess()).isEqualTo(50);
          ctx.completeNow();
        })));
  }

  /* ─── Monthly failed ─── */

  @Test
  @DisplayName("findMonthlyTransactionStatusFailed returns aggregated data")
  void findMonthlyTransactionStatusFailed(VertxTestContext ctx) {
    List<TransactionStats.MonthStatus> list = List.of(
        new TransactionStats.MonthStatus("2026", "Jan", 2L, 100L));
    when(service.getMonthlyStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindMonthlyTransactionStatus.newBuilder()
        .setYear(2026).setMonth(6).build();

    handler.findMonthlyTransactionStatusFailed(req)
        .<ApiResponseTransactionMonthStatusFailed>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalFailed()).isEqualTo(2);
          ctx.completeNow();
        })));
  }

  /* ─── Yearly failed ─── */

  @Test
  @DisplayName("findYearlyTransactionStatusFailed returns aggregated data")
  void findYearlyTransactionStatusFailed(VertxTestContext ctx) {
    List<TransactionStats.YearStatus> list = List.of(
        new TransactionStats.YearStatus("2026", 10L, 500L));
    when(service.getYearlyStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindYearTransactionStatus.newBuilder().setYear(2026).build();

    handler.findYearlyTransactionStatusFailed(req)
        .<ApiResponseTransactionYearStatusFailed>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalFailed()).isEqualTo(10);
          ctx.completeNow();
        })));
  }

  /* ─── Monthly success by card number ─── */

  @Test
  @DisplayName("findMonthlyTransactionStatusSuccessByCardNumber returns aggregated data")
  void findMonthlyTransactionStatusSuccessByCardNumber(VertxTestContext ctx) {
    List<TransactionStats.MonthStatus> list = List.of(
        new TransactionStats.MonthStatus("2026", "Jan", 3L, 300L));
    when(service.getMonthlyStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber.newBuilder()
        .setYear(2026).setMonth(6).setCardNumber("4111111111111111").build();

    handler.findMonthlyTransactionStatusSuccessByCardNumber(req)
        .<ApiResponseTransactionMonthStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── Yearly success by card number ─── */

  @Test
  @DisplayName("findYearlyTransactionStatusSuccessByCardNumber returns aggregated data")
  void findYearlyTransactionStatusSuccessByCardNumber(VertxTestContext ctx) {
    List<TransactionStats.YearStatus> list = List.of(
        new TransactionStats.YearStatus("2026", 20L, 2000L));
    when(service.getYearlyStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindYearTransactionStatusCardNumber.newBuilder()
        .setYear(2026).setCardNumber("4111111111111111").build();

    handler.findYearlyTransactionStatusSuccessByCardNumber(req)
        .<ApiResponseTransactionYearStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── Monthly failed by card number ─── */

  @Test
  @DisplayName("findMonthlyTransactionStatusFailedByCardNumber returns aggregated data")
  void findMonthlyTransactionStatusFailedByCardNumber(VertxTestContext ctx) {
    List<TransactionStats.MonthStatus> list = List.of(
        new TransactionStats.MonthStatus("2026", "Jan", 1L, 50L));
    when(service.getMonthlyStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber.newBuilder()
        .setYear(2026).setMonth(6).setCardNumber("4111111111111111").build();

    handler.findMonthlyTransactionStatusFailedByCardNumber(req)
        .<ApiResponseTransactionMonthStatusFailed>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── Yearly failed by card number ─── */

  @Test
  @DisplayName("findYearlyTransactionStatusFailedByCardNumber returns aggregated data")
  void findYearlyTransactionStatusFailedByCardNumber(VertxTestContext ctx) {
    List<TransactionStats.YearStatus> list = List.of(
        new TransactionStats.YearStatus("2026", 5L, 100L));
    when(service.getYearlyStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindYearTransactionStatusCardNumber.newBuilder()
        .setYear(2026).setCardNumber("4111111111111111").build();

    handler.findYearlyTransactionStatusFailedByCardNumber(req)
        .<ApiResponseTransactionYearStatusFailed>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
