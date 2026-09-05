package io.example.topup.handler;

import io.example.topup.model.TopupStats;
import io.example.topup.service.TopupStatsAmountService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.topup.stats.TopupStatsAmount.ApiResponseTopupMonthAmount;
import pb.topup.stats.TopupStatsAmount.ApiResponseTopupYearAmount;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TopupStatsAmountHandlerTest {

  @Mock
  private TopupStatsAmountService service;

  private TopupStatsAmountHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TopupStatsAmountHandler(service);
  }

  @Test
  @DisplayName("findMonthlyTopupAmounts success")
  void findMonthlyTopupAmounts(VertxTestContext ctx) {
    List<TopupStats.MonthAmount> list = List.of(new TopupStats.MonthAmount("Jan", 100L));
    when(service.getMonthlyTopupAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupStatus.newBuilder().setYear(2026).build();
    handler.findMonthlyTopupAmounts(req)
        .<ApiResponseTopupMonthAmount>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(100);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyTopupAmounts success")
  void findYearlyTopupAmounts(VertxTestContext ctx) {
    List<TopupStats.YearAmount> list = List.of(new TopupStats.YearAmount("2026", 1000L));
    when(service.getYearlyTopupAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupStatus.newBuilder().setYear(2026).build();
    handler.findYearlyTopupAmounts(req)
        .<ApiResponseTopupYearAmount>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(1000);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyTopupAmountsByCardNumber success")
  void findMonthlyTopupAmountsByCardNumber(VertxTestContext ctx) {
    List<TopupStats.MonthAmount> list = List.of(new TopupStats.MonthAmount("Feb", 200L));
    when(service.getMonthlyTopupAmountsByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupCardNumber.newBuilder()
        .setCardNumber("4111111111111111").setYear(2026).build();
    handler.findMonthlyTopupAmountsByCardNumber(req)
        .<ApiResponseTopupMonthAmount>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyTopupAmountsByCardNumber success")
  void findYearlyTopupAmountsByCardNumber(VertxTestContext ctx) {
    List<TopupStats.YearAmount> list = List.of(new TopupStats.YearAmount("2026", 2000L));
    when(service.getYearlyTopupAmountsByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupCardNumber.newBuilder()
        .setCardNumber("4111111111111111").setYear(2026).build();
    handler.findYearlyTopupAmountsByCardNumber(req)
        .<ApiResponseTopupYearAmount>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
