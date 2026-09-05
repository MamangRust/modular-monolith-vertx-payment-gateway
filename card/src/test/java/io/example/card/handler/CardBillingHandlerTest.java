package io.example.card.handler;

import io.example.card.model.BillingStatement;
import io.example.card.service.BillingEngineService;
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
import pb.card.CardBilling;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardBillingHandlerTest {

  @Mock
  private BillingEngineService service;

  private CardBillingHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CardBillingHandler(service);
  }

  private BillingStatement aStatement() {
    return BillingStatement.builder()
        .statementId(1)
        .cardNumber("4111111111111111")
        .statementDate(LocalDate.of(2026, 6, 26))
        .dueDate(LocalDate.of(2026, 7, 26))
        .openingBalance(100000L)
        .purchases(50000L)
        .cashAdvances(0L)
        .payments(100000L)
        .fees(0L)
        .interestCharged(500L)
        .closingBalance(50500L)
        .minimumPayment(10000L)
        .paymentStatus("UNPAID")
        .build();
  }

  @Test
  @DisplayName("triggerBillingCycle returns statement count")
  void triggerBillingCycleSuccess(VertxTestContext ctx) {
    when(service.triggerBillingCycle(15)).thenReturn(Future.succeededFuture(3));

    var req = CardBilling.TriggerBillingRequest.newBuilder().setBillingCycleDay(15).build();

    handler.triggerBillingCycle(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getStatementsGenerated()).isEqualTo(3);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getStatement returns statement if found")
  void getStatementSuccess(VertxTestContext ctx) {
    var stmt = aStatement();
    when(service.getStatement(eq("4111111111111111"), any(LocalDate.class))).thenReturn(Future.succeededFuture(stmt));

    var req = CardBilling.GetStatementRequest.newBuilder()
        .setCardNumber("4111111111111111")
        .setStatementDate("2026-06-26")
        .build();

    handler.getStatement(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getData().getStatementId()).isEqualTo(1);
          assertThat(resp.getData().getClosingBalance()).isEqualTo(50500L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getStatement returns error if not found")
  void getStatementNotFound(VertxTestContext ctx) {
    when(service.getStatement(eq("4111111111111111"), any(LocalDate.class))).thenReturn(Future.succeededFuture(null));

    var req = CardBilling.GetStatementRequest.newBuilder()
        .setCardNumber("4111111111111111")
        .setStatementDate("2026-06-26")
        .build();

    handler.getStatement(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("error");
          assertThat(resp.getMessage()).isEqualTo("Statement not found");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getStatementsByCard returns list of statement results")
  void getStatementsByCardSuccess(VertxTestContext ctx) {
    var stmt = aStatement();
    when(service.getStatementsByCard(eq("4111111111111111"), anyInt(), anyInt())).thenReturn(Future.succeededFuture(List.of(stmt)));

    var req = CardBilling.GetStatementsByCardRequest.newBuilder()
        .setCardNumber("4111111111111111")
        .setPage(1)
        .setPageSize(10)
        .build();

    handler.getStatementsByCard(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getDataList()).hasSize(1);
          assertThat(resp.getTotal()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
