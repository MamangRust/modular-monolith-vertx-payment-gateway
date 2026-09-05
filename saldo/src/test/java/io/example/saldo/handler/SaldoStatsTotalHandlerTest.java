package io.example.saldo.handler;

import io.example.saldo.model.SaldoStats;
import io.example.saldo.service.SaldoStatsTotalService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.saldo.Saldo.FindMonthlySaldoTotalBalance;
import pb.saldo.Saldo.FindYearlySaldo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class SaldoStatsTotalHandlerTest {

  @Mock
  private SaldoStatsTotalService service;

  private SaldoStatsTotalHandler handler;

  @BeforeEach
  void setUp() {
    handler = new SaldoStatsTotalHandler(service);
  }

  @Test
  @DisplayName("findMonthlyTotalSaldoBalance returns list of monthly totals")
  void findMonthlyTotalSaldoBalance(VertxTestContext ctx) {
    var data = List.of(new SaldoStats.MonthTotalBalance("06", "2026", 500_000L));
    when(service.getMonthlyTotalSaldoBalance(any())).thenReturn(Future.succeededFuture(data));

    var req = FindMonthlySaldoTotalBalance.newBuilder().setYear(2026).setMonth(6).build();
    handler.findMonthlyTotalSaldoBalance(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          assertThat(resp.getData(0).getMonth()).isEqualTo("06");
          assertThat(resp.getData(0).getTotalBalance()).isEqualTo(500_000);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearTotalSaldoBalance returns list of yearly totals")
  void findYearTotalSaldoBalance(VertxTestContext ctx) {
    var data = List.of(new SaldoStats.YearTotalBalance("2026", 3_000_000L));
    when(service.getYearlyTotalSaldoBalances(2026)).thenReturn(Future.succeededFuture(data));

    var req = FindYearlySaldo.newBuilder().setYear(2026).build();
    handler.findYearTotalSaldoBalance(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          assertThat(resp.getData(0).getYear()).isEqualTo("2026");
          assertThat(resp.getData(0).getTotalBalance()).isEqualTo(3_000_000);
          ctx.completeNow();
        })));
  }
}
