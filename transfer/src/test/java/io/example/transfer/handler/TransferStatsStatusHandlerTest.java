package io.example.transfer.handler;

import io.example.transfer.model.TransferStats;
import io.example.transfer.service.TransferStatsByCardStatusService;
import io.example.transfer.service.TransferStatsStatusService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.transfer.Transfer.FindMonthlyTransferStatus;
import pb.transfer.Transfer.FindMonthlyTransferStatusCardNumber;
import pb.transfer.Transfer.FindYearTransferStatus;
import pb.transfer.Transfer.FindYearTransferStatusCardNumber;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferMonthStatusSuccess;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferYearStatusSuccess;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TransferStatsStatusHandlerTest {

  @Mock
  private TransferStatsStatusService service;

  @Mock
  private TransferStatsByCardStatusService byCardService;

  private TransferStatsStatusHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TransferStatsStatusHandler(service, byCardService);
  }

  @Test
  @DisplayName("findMonthlyTransferStatusSuccess success")
  void findMonthlyTransferStatusSuccess(VertxTestContext ctx) {
    List<TransferStats.MonthStatus> list = List.of(
        new TransferStats.MonthStatus("2026", "Jun", 10L, 500_000L));
    when(service.getMonthlyTransferStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = FindMonthlyTransferStatus.newBuilder().setYear(2026).setMonth(6).build();
    handler.findMonthlyTransferStatusSuccess(req)
        .<ApiResponseTransferMonthStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalSuccess()).isEqualTo(10);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyTransferStatusSuccess success")
  void findYearlyTransferStatusSuccess(VertxTestContext ctx) {
    List<TransferStats.YearStatus> list = List.of(
        new TransferStats.YearStatus("2026", 120L, 6_000_000L));
    when(service.getYearlyTransferStatus(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearTransferStatus.newBuilder().setYear(2026).build();
    handler.findYearlyTransferStatusSuccess(req)
        .<ApiResponseTransferYearStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalSuccess()).isEqualTo(120);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyTransferStatusSuccessByCardNumber success")
  void findMonthlyTransferStatusSuccessByCardNumber(VertxTestContext ctx) {
    List<TransferStats.MonthStatus> list = List.of(
        new TransferStats.MonthStatus("2026", "Jun", 5L, 250_000L));
    when(byCardService.getMonthlyStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = FindMonthlyTransferStatusCardNumber.newBuilder()
        .setYear(2026).setMonth(6).setCardNumber("4111111111111111").build();
    handler.findMonthlyTransferStatusSuccessByCardNumber(req)
        .<ApiResponseTransferMonthStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalSuccess()).isEqualTo(5);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyTransferStatusSuccessByCardNumber success")
  void findYearlyTransferStatusSuccessByCardNumber(VertxTestContext ctx) {
    List<TransferStats.YearStatus> list = List.of(
        new TransferStats.YearStatus("2026", 60L, 3_000_000L));
    when(byCardService.getYearlyStatusByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearTransferStatusCardNumber.newBuilder()
        .setYear(2026).setCardNumber("4111111111111111").build();
    handler.findYearlyTransferStatusSuccessByCardNumber(req)
        .<ApiResponseTransferYearStatusSuccess>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalSuccess()).isEqualTo(60);
          ctx.completeNow();
        })));
  }
}
