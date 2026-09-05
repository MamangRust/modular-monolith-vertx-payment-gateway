package io.example.merchant.handler;

import io.example.merchant.model.MerchantStats;
import io.example.merchant.service.MerchantStatsAmountByApiKeyService;
import io.example.merchant.service.MerchantStatsAmountByMerchantService;
import io.example.merchant.service.MerchantStatsAmountService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.merchant.Merchant.FindYearMerchant;
import pb.merchant.Merchant.FindYearMerchantByApikey;
import pb.merchant.Merchant.FindYearMerchantById;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class MerchantStatsAmountHandlerTest {

  @Mock
  private MerchantStatsAmountService globalService;

  @Mock
  private MerchantStatsAmountByApiKeyService apiKeyService;

  @Mock
  private MerchantStatsAmountByMerchantService merchantService;

  private MerchantStatsAmountHandler handler;

  @BeforeEach
  void setUp() {
    handler = new MerchantStatsAmountHandler(globalService, apiKeyService, merchantService);
  }

  @Test
  @DisplayName("findMonthlyAmountMerchant success")
  void findMonthlyAmountMerchantSuccess(VertxTestContext ctx) {
    List<MerchantStats.MonthAmount> list = List.of(new MerchantStats.MonthAmount("Jan", 100L));
    when(globalService.getMonthlyAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    handler.findMonthlyAmountMerchant(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(100L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyAmountMerchant success")
  void findYearlyAmountMerchantSuccess(VertxTestContext ctx) {
    List<MerchantStats.YearAmount> list = List.of(new MerchantStats.YearAmount("2026", 1000L));
    when(globalService.getYearlyAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    handler.findYearlyAmountMerchant(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(1000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyAmountByMerchants success")
  void findMonthlyAmountByMerchantsSuccess(VertxTestContext ctx) {
    List<MerchantStats.MonthAmount> list = List.of(new MerchantStats.MonthAmount("Feb", 200L));
    when(merchantService.getMonthlyAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantById.newBuilder().setMerchantId(5).setYear(2026).build();
    handler.findMonthlyAmountByMerchants(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyAmountByMerchants success")
  void findYearlyAmountByMerchantsSuccess(VertxTestContext ctx) {
    List<MerchantStats.YearAmount> list = List.of(new MerchantStats.YearAmount("2026", 2000L));
    when(merchantService.getYearlyAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantById.newBuilder().setMerchantId(5).setYear(2026).build();
    handler.findYearlyAmountByMerchants(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyAmountByApikey success")
  void findMonthlyAmountByApikeySuccess(VertxTestContext ctx) {
    List<MerchantStats.MonthAmount> list = List.of(new MerchantStats.MonthAmount("Mar", 300L));
    when(apiKeyService.getMonthlyAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantByApikey.newBuilder().setApiKey("key").setYear(2026).build();
    handler.findMonthlyAmountByApikey(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyAmountByApikey success")
  void findYearlyAmountByApikeySuccess(VertxTestContext ctx) {
    List<MerchantStats.YearAmount> list = List.of(new MerchantStats.YearAmount("2026", 3000L));
    when(apiKeyService.getYearlyAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantByApikey.newBuilder().setApiKey("key").setYear(2026).build();
    handler.findYearlyAmountByApikey(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }
}
