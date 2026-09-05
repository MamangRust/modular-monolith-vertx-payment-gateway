package io.example.merchant.handler;

import io.example.merchant.model.MerchantStats;
import io.example.merchant.service.MerchantStatsTotalAmountByApiKeyService;
import io.example.merchant.service.MerchantStatsTotalAmountByMerchantService;
import io.example.merchant.service.MerchantStatsTotalAmountService;
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
class MerchantStatsTotalAmountHandlerTest {

  @Mock
  private MerchantStatsTotalAmountService globalService;

  @Mock
  private MerchantStatsTotalAmountByApiKeyService apiKeyService;

  @Mock
  private MerchantStatsTotalAmountByMerchantService merchantService;

  private MerchantStatsTotalAmountHandler handler;

  @BeforeEach
  void setUp() {
    handler = new MerchantStatsTotalAmountHandler(globalService, apiKeyService, merchantService);
  }

  @Test
  @DisplayName("findMonthlyTotalAmountMerchant success")
  void findMonthlyTotalAmountMerchantSuccess(VertxTestContext ctx) {
    List<MerchantStats.MonthAmount> list = List.of(new MerchantStats.MonthAmount("Jan", 100L));
    when(globalService.getMonthlyTotalAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    handler.findMonthlyTotalAmountMerchant(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getTotalAmount()).isEqualTo(100L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyTotalAmountMerchant success")
  void findYearlyTotalAmountMerchantSuccess(VertxTestContext ctx) {
    List<MerchantStats.YearAmount> list = List.of(new MerchantStats.YearAmount("2026", 1000L));
    when(globalService.getYearlyTotalAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchant.newBuilder().setYear(2026).build();
    handler.findYearlyTotalAmountMerchant(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyTotalAmountByMerchants success")
  void findMonthlyTotalAmountByMerchantsSuccess(VertxTestContext ctx) {
    List<MerchantStats.MonthAmount> list = List.of();
    when(merchantService.getMonthlyTotalAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantById.newBuilder().setMerchantId(5).setYear(2026).build();
    handler.findMonthlyTotalAmountByMerchants(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyTotalAmountByMerchants success")
  void findYearlyTotalAmountByMerchantsSuccess(VertxTestContext ctx) {
    List<MerchantStats.YearAmount> list = List.of();
    when(merchantService.getYearlyTotalAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantById.newBuilder().setMerchantId(5).setYear(2026).build();
    handler.findYearlyTotalAmountByMerchants(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findMonthlyTotalAmountByApikey success")
  void findMonthlyTotalAmountByApikeySuccess(VertxTestContext ctx) {
    List<MerchantStats.MonthAmount> list = List.of();
    when(apiKeyService.getMonthlyTotalAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantByApikey.newBuilder().setApiKey("key").setYear(2026).build();
    handler.findMonthlyTotalAmountByApikey(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findYearlyTotalAmountByApikey success")
  void findYearlyTotalAmountByApikeySuccess(VertxTestContext ctx) {
    List<MerchantStats.YearAmount> list = List.of();
    when(apiKeyService.getYearlyTotalAmounts(any())).thenReturn(Future.succeededFuture(list));

    var req = FindYearMerchantByApikey.newBuilder().setApiKey("key").setYear(2026).build();
    handler.findYearlyTotalAmountByApikey(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }
}
