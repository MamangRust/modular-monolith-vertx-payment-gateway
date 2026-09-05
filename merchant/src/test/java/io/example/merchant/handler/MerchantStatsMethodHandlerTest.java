package io.example.merchant.handler;

import io.example.merchant.model.MerchantStats;
import io.example.merchant.service.MerchantStatsMethodByApiKeyService;
import io.example.merchant.service.MerchantStatsMethodByMerchantService;
import io.example.merchant.service.MerchantStatsMethodService;
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
class MerchantStatsMethodHandlerTest {

  @Mock
  private MerchantStatsMethodService globalService;

  @Mock
  private MerchantStatsMethodByApiKeyService apiKeyService;

  @Mock
  private MerchantStatsMethodByMerchantService merchantService;

  private MerchantStatsMethodHandler handler;

  @BeforeEach
  void setUp() {
    handler = new MerchantStatsMethodHandler(globalService, apiKeyService, merchantService);
  }

  @Test
  @DisplayName("findMonthlyPaymentMethodsMerchant success")
  void findMonthlyPaymentMethodsMerchantSuccess(VertxTestContext ctx) {
    List<MerchantStats.MonthMethod> list = List.of(new MerchantStats.MonthMethod("Jan", "CREDIT", 100L));
    when(globalService.getMonthlyMethodAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    handler.findMonthlyPaymentMethodsMerchant(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getPaymentMethod()).isEqualTo("CREDIT");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyPaymentMethodMerchant success")
  void findYearlyPaymentMethodMerchantSuccess(VertxTestContext ctx) {
    List<MerchantStats.YearMethod> list = List.of(new MerchantStats.YearMethod("2026", "DEBIT", 1000L));
    when(globalService.getYearlyMethodAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    handler.findYearlyPaymentMethodMerchant(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyPaymentMethodByMerchants success")
  void findMonthlyPaymentMethodByMerchantsSuccess(VertxTestContext ctx) {
    List<MerchantStats.MonthMethod> list = List.of();
    when(merchantService.getMonthlyMethodAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantById.newBuilder().setMerchantId(5).setYear(2026).build();
    handler.findMonthlyPaymentMethodByMerchants(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyPaymentMethodByMerchants success")
  void findYearlyPaymentMethodByMerchantsSuccess(VertxTestContext ctx) {
    List<MerchantStats.YearMethod> list = List.of();
    when(merchantService.getYearlyMethodAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantById.newBuilder().setMerchantId(5).setYear(2026).build();
    handler.findYearlyPaymentMethodByMerchants(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyPaymentMethodByApikey success")
  void findMonthlyPaymentMethodByApikeySuccess(VertxTestContext ctx) {
    List<MerchantStats.MonthMethod> list = List.of();
    when(apiKeyService.getMonthlyMethodAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantByApikey.newBuilder().setApiKey("key").setYear(2026).build();
    handler.findMonthlyPaymentMethodByApikey(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyPaymentMethodByApikey success")
  void findYearlyPaymentMethodByApikeySuccess(VertxTestContext ctx) {
    List<MerchantStats.YearMethod> list = List.of();
    when(apiKeyService.getYearlyMethodAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantByApikey.newBuilder().setApiKey("key").setYear(2026).build();
    handler.findYearlyPaymentMethodByApikey(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }
}
