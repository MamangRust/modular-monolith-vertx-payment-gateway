package io.example.transaction.handler;

import io.example.transaction.model.TransactionStats;
import io.example.transaction.service.TransactionStatsAmountService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount;
import pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TransactionStatsAmountHandlerTest {

  @Mock
  private TransactionStatsAmountService service;

  private TransactionStatsAmountHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TransactionStatsAmountHandler(service);
  }

  @Test
  @DisplayName("findMonthlyAmounts success")
  void findMonthlyAmounts(VertxTestContext ctx) {
    List<TransactionStats.MonthAmount> list = List.of(new TransactionStats.MonthAmount("Jan", 100L));
    when(service.getMonthlyAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindYearTransactionStatus.newBuilder().setYear(2026).build();
    handler.findMonthlyAmounts(req)
        .<ApiResponseTransactionMonthAmount>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(100);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyAmounts success")
  void findYearlyAmounts(VertxTestContext ctx) {
    List<TransactionStats.YearAmount> list = List.of(new TransactionStats.YearAmount("2026", 1000L));
    when(service.getYearlyAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindYearTransactionStatus.newBuilder().setYear(2026).build();
    handler.findYearlyAmounts(req)
        .<ApiResponseTransactionYearAmount>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(1000);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyAmountsByCardNumber success")
  void findMonthlyAmountsByCardNumber(VertxTestContext ctx) {
    List<TransactionStats.MonthAmount> list = List.of(new TransactionStats.MonthAmount("Feb", 200L));
    when(service.getMonthlyAmountsByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
        .setCardNumber("4111111111111111").setYear(2026).build();
    handler.findMonthlyAmountsByCardNumber(req)
        .<ApiResponseTransactionMonthAmount>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyAmountsByCardNumber success")
  void findYearlyAmountsByCardNumber(VertxTestContext ctx) {
    List<TransactionStats.YearAmount> list = List.of(new TransactionStats.YearAmount("2026", 2000L));
    when(service.getYearlyAmountsByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
        .setCardNumber("4111111111111111").setYear(2026).build();
    handler.findYearlyAmountsByCardNumber(req)
        .<ApiResponseTransactionYearAmount>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
