package io.example.card.service.impl;

import io.example.card.model.CardCreditAccount;
import io.example.card.model.CardPayment;
import io.example.card.repository.CardCreditAccountRepository;
import io.example.card.repository.CardPaymentRepository;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardPaymentServiceImplTest {

  @Mock
  private CardPaymentRepository paymentRepo;

  @Mock
  private CardCreditAccountRepository creditAccountRepo;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  @Mock
  private KafkaService kafkaService;

  private CardPaymentServiceImpl service;
  private final UUID paymentUuid = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new CardPaymentServiceImpl(paymentRepo, creditAccountRepo, redis, metrics, kafkaService);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(ctx);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(ctx);
  }

  private CardPayment aPayment() {
    return CardPayment.builder()
        .paymentId(paymentUuid)
        .referenceId("ref-123")
        .cardNumber("4111111111111111")
        .amount(50000L)
        .paymentChannel("BANK_TRANSFER")
        .status("POSTED")
        .statementId(1)
        .build();
  }

  @Test
  @DisplayName("postPayment returns existing payment if already posted")
  void postPaymentIdempotent(VertxTestContext ctx) {
    mockTracing();
    var existing = aPayment();
    when(paymentRepo.findByReferenceId("ref-123")).thenReturn(Future.succeededFuture(existing));

    service.postPayment("ref-123", "4111111111111111", 50000L, "BANK_TRANSFER", 1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getReferenceId()).isEqualTo("ref-123");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("postPayment inserts payment, releases credit, deletes cache, and publishes event")
  void postPaymentSuccess(VertxTestContext ctx) {
    mockTracing();
    var payment = aPayment();
    var account = CardCreditAccount.builder().cardNumber("4111111111111111").build();

    when(paymentRepo.findByReferenceId("ref-123")).thenReturn(Future.succeededFuture(null));
    when(paymentRepo.insertPayment(any(CardPayment.class))).thenReturn(Future.succeededFuture(payment));
    when(creditAccountRepo.releaseCredit("4111111111111111", 50000L)).thenReturn(Future.succeededFuture(account));
    when(redis.delete("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(1L));
    when(kafkaService.sendMessage(eq("card.payment.posted"), eq("ref-123"), any())).thenReturn(Future.succeededFuture());

    service.postPayment("ref-123", "4111111111111111", 50000L, "BANK_TRANSFER", 1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getReferenceId()).isEqualTo("ref-123");
          verify(redis).delete("card:4111111111111111:credit");
          verify(kafkaService).sendMessage(eq("card.payment.posted"), eq("ref-123"), any());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getPaymentHistory returns history list")
  void getPaymentHistorySuccess(VertxTestContext ctx) {
    mockTracing();
    var payment = aPayment();
    when(paymentRepo.findByCardNumber("4111111111111111", 10, 0)).thenReturn(Future.succeededFuture(List.of(payment)));

    service.getPaymentHistory("4111111111111111", 1, 10)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("countPayments returns total payments count")
  void countPaymentsSuccess(VertxTestContext ctx) {
    when(paymentRepo.countByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(5));

    service.countPayments("4111111111111111")
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(5);
          ctx.completeNow();
        })));
  }
}
