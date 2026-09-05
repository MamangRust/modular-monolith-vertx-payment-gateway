package io.example.merchant.service;

import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.observability.TracingMetrics;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.model.MerchantTransactions;
import io.example.merchant.repository.MerchantStatsRepository;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MerchantStatsServiceTest {

  @Mock
  private MerchantStatsRepository repo;

  @Mock
  private TracingMetrics tracingMetrics;

  private MerchantStatsService service;

  @BeforeEach
  void setUp() {
    service = new MerchantStatsService(repo, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(io.opentelemetry.context.Context.root(), java.time.Instant.now());
    when(tracingMetrics.startSpan(any(String.class))).thenReturn(tc);
    when(tracingMetrics.startSpan(any(String.class), any())).thenReturn(tc);
  }

  @Test
  @DisplayName("getTransactions success")
  void getTransactionsSuccess(VertxTestContext ctx) {
    mockTracing();
    PagedResult<MerchantTransactions> paged = new PagedResult<>(List.of(), 0);
    when(repo.getTransactions(1, 10, "search")).thenReturn(Future.succeededFuture(paged));

    service.getTransactions(1, 10, "search")
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getData()).isEmpty();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransactionsByMerchantId success")
  void getTransactionsByMerchantIdSuccess(VertxTestContext ctx) {
    mockTracing();
    PagedResult<MerchantTransactions> paged = new PagedResult<>(List.of(), 0);
    when(repo.getTransactionsByMerchantId(1, 10, "search", 5)).thenReturn(Future.succeededFuture(paged));

    service.getTransactionsByMerchantId(1, 10, "search", 5)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getData()).isEmpty();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransactionsByApiKey success")
  void getTransactionsByApiKeySuccess(VertxTestContext ctx) {
    mockTracing();
    PagedResult<MerchantTransactions> paged = new PagedResult<>(List.of(), 0);
    when(repo.getTransactionsByApiKey(1, 10, "search", "key")).thenReturn(Future.succeededFuture(paged));

    service.getTransactionsByApiKey(1, 10, "search", "key")
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getData()).isEmpty();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyAmounts success")
  void getMonthlyAmountsSuccess(VertxTestContext ctx) {
    mockTracing();
    List<MerchantStats.MonthAmount> list = List.of(new MerchantStats.MonthAmount("Jan", 100L));
    when(repo.getMonthlyAmounts(2026, 5, "key")).thenReturn(Future.succeededFuture(list));

    service.getMonthlyAmounts(2026, 5, "key")
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          assertThat(res.get(0).getAmount()).isEqualTo(100L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyAmounts success")
  void getYearlyAmountsSuccess(VertxTestContext ctx) {
    mockTracing();
    List<MerchantStats.YearAmount> list = List.of(new MerchantStats.YearAmount("2026", 1000L));
    when(repo.getYearlyAmounts(2026, 5, "key")).thenReturn(Future.succeededFuture(list));

    service.getYearlyAmounts(2026, 5, "key")
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyMethodAmounts success")
  void getMonthlyMethodAmountsSuccess(VertxTestContext ctx) {
    mockTracing();
    List<MerchantStats.MonthMethod> list = List.of();
    when(repo.getMonthlyMethodAmounts(2026, 5, "key")).thenReturn(Future.succeededFuture(list));

    service.getMonthlyMethodAmounts(2026, 5, "key")
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isEmpty();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyMethodAmounts success")
  void getYearlyMethodAmountsSuccess(VertxTestContext ctx) {
    mockTracing();
    List<MerchantStats.YearMethod> list = List.of();
    when(repo.getYearlyMethodAmounts(2026, 5, "key")).thenReturn(Future.succeededFuture(list));

    service.getYearlyMethodAmounts(2026, 5, "key")
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isEmpty();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getMonthlyTotalAmounts success")
  void getMonthlyTotalAmountsSuccess(VertxTestContext ctx) {
    mockTracing();
    List<MerchantStats.MonthAmount> list = List.of();
    when(repo.getMonthlyTotalAmounts(2026, 5, "key")).thenReturn(Future.succeededFuture(list));

    service.getMonthlyTotalAmounts(2026, 5, "key")
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isEmpty();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getYearlyTotalAmounts success")
  void getYearlyTotalAmountsSuccess(VertxTestContext ctx) {
    mockTracing();
    List<MerchantStats.YearAmount> list = List.of();
    when(repo.getYearlyTotalAmounts(2026, 5, "key")).thenReturn(Future.succeededFuture(list));

    service.getYearlyTotalAmounts(2026, 5, "key")
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isEmpty();
          ctx.completeNow();
        })));
  }
}
