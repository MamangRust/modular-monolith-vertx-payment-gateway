package io.example.card.handler;

import io.example.card.model.CardCreditAccount;
import io.example.card.service.CreditLimitService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pb.card.CardLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardLimitHandlerTest {

  @Mock
  private CreditLimitService service;

  private CardLimitHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CardLimitHandler(service);
  }

  private CardCreditAccount aCreditAccount() {
    return CardCreditAccount.builder()
        .cardNumber("4111111111111111")
        .creditLimit(10000000L)
        .usedCredit(2000000L)
        .availableCredit(8000000L)
        .status("ACTIVE")
        .billingCycleDay(15)
        .annualRateBps(1800)
        .build();
  }

  @Test
  @DisplayName("getLimit returns credit limit details")
  void getLimitSuccess(VertxTestContext ctx) {
    var account = aCreditAccount();
    when(service.getLimit("4111111111111111")).thenReturn(Future.succeededFuture(account));

    var req = CardLimit.GetLimitByCardNumberRequest.newBuilder().setCardNumber("4111111111111111").build();

    handler.getLimit(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getData().getCreditLimit()).isEqualTo(10000000L);
          assertThat(resp.getData().getAvailableCredit()).isEqualTo(8000000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("setLimit configures and returns credit limit details")
  void setLimitSuccess(VertxTestContext ctx) {
    var account = aCreditAccount();
    when(service.setLimit(anyString(), anyLong(), any(), any())).thenReturn(Future.succeededFuture(account));

    var req = CardLimit.SetLimitRequest.newBuilder()
        .setCardNumber("4111111111111111")
        .setCreditLimit(10000000L)
        .setBillingCycleDay(15)
        .setAnnualRateBps(1800)
        .build();

    handler.setLimit(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getData().getCreditLimit()).isEqualTo(10000000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("adjustLimit adjusts and returns credit limit details")
  void adjustLimitSuccess(VertxTestContext ctx) {
    var account = aCreditAccount();
    account.setCreditLimit(12000000L);
    account.setAvailableCredit(10000000L);
    when(service.adjustLimit("4111111111111111", 2000000L)).thenReturn(Future.succeededFuture(account));

    var req = CardLimit.AdjustLimitRequest.newBuilder()
        .setCardNumber("4111111111111111")
        .setDelta(2000000L)
        .build();

    handler.adjustLimit(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getData().getCreditLimit()).isEqualTo(12000000L);
          ctx.completeNow();
        })));
  }
}
