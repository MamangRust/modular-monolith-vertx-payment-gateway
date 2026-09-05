package io.example.card.handler;

import io.example.card.model.CardPayment;
import io.example.card.service.CardPaymentService;
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
import pb.card.CardPayment.GetPaymentHistoryRequest;
import pb.card.CardPayment.PostPaymentRequest;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardPaymentHandlerTest {

  @Mock
  private CardPaymentService service;

  private CardPaymentHandler handler;
  private final UUID paymentUuid = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    handler = new CardPaymentHandler(service);
  }

  private CardPayment aPayment() {
    return CardPayment.builder()
        .paymentId(paymentUuid)
        .referenceId("ref-123")
        .cardNumber("4111111111111111")
        .amount(50000L)
        .paymentChannel("BANK_TRANSFER")
        .paymentTime(Timestamp.from(Instant.now()))
        .status("POSTED")
        .statementId(1)
        .createdAt(Timestamp.from(Instant.now()))
        .build();
  }

  @Test
  @DisplayName("postPayment delegates to service and returns PostPaymentResponse")
  void postPaymentSuccess(VertxTestContext ctx) {
    var payment = aPayment();
    when(service.postPayment(anyString(), anyString(), any(), anyString(), any())).thenReturn(Future.succeededFuture(payment));

    var req = PostPaymentRequest.newBuilder()
        .setReferenceId("ref-123")
        .setCardNumber("4111111111111111")
        .setAmount(50000L)
        .setPaymentChannel("BANK_TRANSFER")
        .setStatementId(1)
        .build();

    handler.postPayment(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getData().getPaymentId()).isEqualTo(paymentUuid.toString());
          assertThat(resp.getData().getReferenceId()).isEqualTo("ref-123");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getPaymentHistory delegates to service and returns list of history entries")
  void getPaymentHistorySuccess(VertxTestContext ctx) {
    var payment = aPayment();
    when(service.getPaymentHistory(eq("4111111111111111"), anyInt(), anyInt())).thenReturn(Future.succeededFuture(List.of(payment)));
    when(service.countPayments("4111111111111111")).thenReturn(Future.succeededFuture(5));

    var req = GetPaymentHistoryRequest.newBuilder()
        .setCardNumber("4111111111111111")
        .setPage(1)
        .setPageSize(10)
        .build();

    handler.getPaymentHistory(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getDataList()).hasSize(1);
          assertThat(resp.getTotal()).isEqualTo(5);
          ctx.completeNow();
        })));
  }
}
