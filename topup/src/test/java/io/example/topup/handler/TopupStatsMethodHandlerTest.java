package io.example.topup.handler;

import io.example.topup.model.TopupStats;
import io.example.topup.service.TopupStatsMethodService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.topup.stats.TopupStatsMethod.ApiResponseTopupMonthMethod;
import pb.topup.stats.TopupStatsMethod.ApiResponseTopupYearMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TopupStatsMethodHandlerTest {

  @Mock
  private TopupStatsMethodService service;

  private TopupStatsMethodHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TopupStatsMethodHandler(service);
  }

  @Test
  @DisplayName("findMonthlyTopupMethods success")
  void findMonthlyTopupMethods(VertxTestContext ctx) {
    List<TopupStats.MonthMethod> list = List.of(
        new TopupStats.MonthMethod("Jan", "CREDIT_CARD", 10L, 1000L));
    when(service.getMonthlyTopupMethods(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupStatus.newBuilder().setYear(2026).build();
    handler.findMonthlyTopupMethods(req)
        .<ApiResponseTopupMonthMethod>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTopupMethod()).isEqualTo("CREDIT_CARD");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyTopupMethods success")
  void findYearlyTopupMethods(VertxTestContext ctx) {
    List<TopupStats.YearMethod> list = List.of(
        new TopupStats.YearMethod("2026", "DEBIT", 5L, 500L));
    when(service.getYearlyTopupMethods(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupStatus.newBuilder().setYear(2026).build();
    handler.findYearlyTopupMethods(req)
        .<ApiResponseTopupYearMethod>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTopupMethod()).isEqualTo("DEBIT");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyTopupMethodsByCardNumber success")
  void findMonthlyTopupMethodsByCardNumber(VertxTestContext ctx) {
    List<TopupStats.MonthMethod> list = List.of();
    when(service.getMonthlyTopupMethodsByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupCardNumber.newBuilder()
        .setCardNumber("4111111111111111").setYear(2026).build();
    handler.findMonthlyTopupMethodsByCardNumber(req)
        .<ApiResponseTopupMonthMethod>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyTopupMethodsByCardNumber success")
  void findYearlyTopupMethodsByCardNumber(VertxTestContext ctx) {
    List<TopupStats.YearMethod> list = List.of();
    when(service.getYearlyTopupMethodsByCard(any())).thenReturn(Future.succeededFuture(list));

    var req = pb.topup.Topup.FindYearTopupCardNumber.newBuilder()
        .setCardNumber("4111111111111111").setYear(2026).build();
    handler.findYearlyTopupMethodsByCardNumber(req)
        .<ApiResponseTopupYearMethod>onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }
}
