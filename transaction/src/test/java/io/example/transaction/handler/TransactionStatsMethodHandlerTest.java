package io.example.transaction.handler;

import io.example.transaction.model.TransactionStats;
import io.example.transaction.service.TransactionStatsMethodService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionMonthMethod;
import pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionYearMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TransactionStatsMethodHandlerTest {

  @Mock
  private TransactionStatsMethodService service;

  private TransactionStatsMethodHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TransactionStatsMethodHandler(service);
  }

  @Test
  @DisplayName("findMonthlyPaymentMethods success")
  void findMonthlyPaymentMethods(VertxTestContext ctx) {
    List<TransactionStats.MonthMethod> list = List.of(
        new TransactionStats.MonthMethod("Jan", "CREDIT_CARD", 10L, 1000L));
    when(service.getMonthlyMethods(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindYearTransactionStatus.newBuilder().setYear(2026).build();
    handler.findMonthlyPaymentMethods(req)
        .<ApiResponseTransactionMonthMethod>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getPaymentMethod()).isEqualTo("CREDIT_CARD");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyPaymentMethods success")
  void findYearlyPaymentMethods(VertxTestContext ctx) {
    List<TransactionStats.YearMethod> list = List.of(
        new TransactionStats.YearMethod("2026", "DEBIT", 5L, 500L));
    when(service.getYearlyMethods(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindYearTransactionStatus.newBuilder().setYear(2026).build();
    handler.findYearlyPaymentMethods(req)
        .<ApiResponseTransactionYearMethod>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getPaymentMethod()).isEqualTo("DEBIT");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyPaymentMethodsByCardNumber success")
  void findMonthlyPaymentMethodsByCardNumber(VertxTestContext ctx) {
    List<TransactionStats.MonthMethod> list = List.of();
    when(service.getMonthlyMethodsByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
        .setCardNumber("4111111111111111").setYear(2026).build();
    handler.findMonthlyPaymentMethodsByCardNumber(req)
        .<ApiResponseTransactionMonthMethod>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyPaymentMethodsByCardNumber success")
  void findYearlyPaymentMethodsByCardNumber(VertxTestContext ctx) {
    List<TransactionStats.YearMethod> list = List.of();
    when(service.getYearlyMethodsByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
        .setCardNumber("4111111111111111").setYear(2026).build();
    handler.findYearlyPaymentMethodsByCardNumber(req)
        .<ApiResponseTransactionYearMethod>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }
}
