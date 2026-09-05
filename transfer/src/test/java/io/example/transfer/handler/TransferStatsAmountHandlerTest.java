package io.example.transfer.handler;

import io.example.transfer.model.TransferStats;
import io.example.transfer.service.TransferStatsAmountService;
import io.example.transfer.service.TransferStatsByCardAmountService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.transfer.Transfer.FindYearTransferStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TransferStatsAmountHandlerTest {

  @Mock
  private TransferStatsAmountService service;

  @Mock
  private TransferStatsByCardAmountService byCardService;

  private TransferStatsAmountHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TransferStatsAmountHandler(service, byCardService);
  }

  @Test
  @DisplayName("findMonthlyTransferAmounts success")
  void findMonthlyTransferAmountsSuccess(VertxTestContext ctx) {
    List<TransferStats.MonthAmount> list = List.of(new TransferStats.MonthAmount("June", 500_000L));
    when(service.getMonthlyTransferAmounts(2026)).thenReturn(Future.succeededFuture(list));

    var req = FindYearTransferStatus.newBuilder().setYear(2026).build();
    handler.findMonthlyTransferAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(500_000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyTransferAmounts success")
  void findYearlyTransferAmountsSuccess(VertxTestContext ctx) {
    List<TransferStats.YearAmount> list = List.of(new TransferStats.YearAmount("2026", 5_000_000L));
    when(service.getYearlyTransferAmounts(2026)).thenReturn(Future.succeededFuture(list));

    var req = FindYearTransferStatus.newBuilder().setYear(2026).build();
    handler.findYearlyTransferAmounts(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(5_000_000L);
          ctx.completeNow();
        })));
  }
}
