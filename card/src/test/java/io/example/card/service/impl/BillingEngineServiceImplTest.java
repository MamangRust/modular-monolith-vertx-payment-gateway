package io.example.card.service.impl;

import io.example.card.model.BillingStatement;
import io.example.card.model.CardCreditAccount;
import io.example.card.repository.BillingStatementRepository;
import io.example.card.repository.CardCreditAccountRepository;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class BillingEngineServiceImplTest {

  @Mock
  private CardCreditAccountRepository creditAccountRepo;

  @Mock
  private BillingStatementRepository statementRepo;

  @Mock
  private TracingMetrics metrics;

  @Mock
  private KafkaService kafkaService;

  private BillingEngineServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new BillingEngineServiceImpl(creditAccountRepo, statementRepo, metrics, kafkaService);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(ctx);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(ctx);
  }

  private CardCreditAccount aCreditAccount() {
    return CardCreditAccount.builder()
        .cardNumber("4111111111111111")
        .creditLimit(10000000L)
        .usedCredit(2000000L)
        .billingCycleDay(15)
        .paymentDueDays(20)
        .annualRateBps(1800)
        .status("ACTIVE")
        .lastStatementDate(LocalDate.now().minusMonths(1))
        .build();
  }

  private BillingStatement aStatement() {
    return BillingStatement.builder()
        .statementId(1)
        .cardNumber("4111111111111111")
        .statementDate(LocalDate.now())
        .closingBalance(2000000L)
        .minimumPayment(100000L)
        .paymentStatus("UNPAID")
        .build();
  }

  @Test
  @DisplayName("triggerBillingCycle returns 0 when no accounts due")
  void triggerBillingCycleEmpty(VertxTestContext ctx) {
    mockTracing();
    when(creditAccountRepo.findAccountsDueForBilling(15)).thenReturn(Future.succeededFuture(List.of()));

    service.triggerBillingCycle(15)
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isZero();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("triggerBillingCycle generates and inserts statement when accounts due")
  void triggerBillingCycleSuccess(VertxTestContext ctx) {
    mockTracing();
    var account = aCreditAccount();
    var stmt = aStatement();

    when(creditAccountRepo.findAccountsDueForBilling(15)).thenReturn(Future.succeededFuture(List.of(account)));
    when(statementRepo.insertStatement(any(BillingStatement.class))).thenReturn(Future.succeededFuture(stmt));
    when(creditAccountRepo.findByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(account));
    when(kafkaService.sendMessage(eq("card.statement.generated"), anyString(), any())).thenReturn(Future.succeededFuture());

    service.triggerBillingCycle(15)
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(1);
          verify(kafkaService).sendMessage(eq("card.statement.generated"), anyString(), any());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getStatement returns statement if found")
  void getStatementSuccess(VertxTestContext ctx) {
    var stmt = aStatement();
    var date = LocalDate.now();
    when(statementRepo.findByCardAndCycle("4111111111111111", date)).thenReturn(Future.succeededFuture(stmt));

    service.getStatement("4111111111111111", date)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getStatementsByCard returns list of statements")
  void getStatementsByCardSuccess(VertxTestContext ctx) {
    var stmt = aStatement();
    when(statementRepo.findByCardNumber("4111111111111111", 10, 0)).thenReturn(Future.succeededFuture(List.of(stmt)));

    service.getStatementsByCard("4111111111111111", 1, 10)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          ctx.completeNow();
        })));
  }
}
