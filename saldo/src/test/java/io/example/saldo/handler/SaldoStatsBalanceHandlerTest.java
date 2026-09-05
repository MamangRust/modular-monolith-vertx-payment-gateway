package io.example.saldo.handler;

import io.example.saldo.model.SaldoStats;
import io.example.saldo.service.SaldoStatsBalanceService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.saldo.Saldo.FindYearlySaldo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class SaldoStatsBalanceHandlerTest {

  @Mock
  private SaldoStatsBalanceService service;

  private SaldoStatsBalanceHandler handler;

  @BeforeEach
  void setUp() {
    handler = new SaldoStatsBalanceHandler(service);
  }

  @Test
  @DisplayName("findMonthlySaldoBalances returns list of monthly balances")
  void findMonthlySaldoBalances(VertxTestContext ctx) {
    var data = List.of(new SaldoStats.MonthBalance("06", 500_000L));
    when(service.getMonthlySaldoBalances(2026)).thenReturn(Future.succeededFuture(data));

    var req = FindYearlySaldo.newBuilder().setYear(2026).build();
    handler.findMonthlySaldoBalances(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          assertThat(resp.getData(0).getMonth()).isEqualTo("06");
          assertThat(resp.getData(0).getTotalBalance()).isEqualTo(500_000);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlySaldoBalances returns list of yearly balances")
  void findYearlySaldoBalances(VertxTestContext ctx) {
    var data = List.of(new SaldoStats.YearBalance("2026", 3_000_000L));
    when(service.getYearlySaldoBalances(2026)).thenReturn(Future.succeededFuture(data));

    var req = FindYearlySaldo.newBuilder().setYear(2026).build();
    handler.findYearlySaldoBalances(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          assertThat(resp.getData(0).getYear()).isEqualTo("2026");
          assertThat(resp.getData(0).getTotalBalance()).isEqualTo(3_000_000);
          ctx.completeNow();
        })));
  }
}
